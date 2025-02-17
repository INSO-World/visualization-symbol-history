package com.mategka.dava.analyzer.spoon.action;

import com.mategka.dava.analyzer.collections.ChainMap;
import com.mategka.dava.analyzer.extension.*;
import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.struct.symbol.ElementCapture;
import com.mategka.dava.analyzer.struct.symbol.Subject;

import com.google.common.collect.BiMap;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.*;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@UtilityClass
public class EditActions {

  public List<EditAction> fromDiff(
    Diff astDiff,
    BiMap<CtElement, CtElement> mappings
  ) {
    var rawActions = astDiff.getRootOperations().stream()
      .map(EditActions::fromOperation)
      .map(Covariant::<EditAction>list)
      .flatMap(Collection::stream)
      .distinct()
      .toList();
    var deletionsMap = Streamer.ofCollection(rawActions)
      .narrow(DeletionAction.class)
      .map(Pair.fromRight(DeletionAction::getOldElement))
      .filter(p -> mappings.containsKey(p.left()))
      .collect(CollectorsX.pairsToMap());
    var replacement = findRootReplacement(rawActions);
    var updateMappingKeys = SetsX.difference(mappings.keySet(), deletionsMap.keySet());
    var updatesStream = PairStream.mapping(mappings.entrySet(), Pair::fromEntry)
      .filterLeft(updateMappingKeys::contains)
      .filterBoth(ElementCapture.CLASSES_TO_CAPTURE::containsClassOf)
      .map(elements -> {
        var oldParent = ElementCapture.getNearestValidParent(elements.left());
        var newParent = ElementCapture.getNearestValidParent(elements.right());
        return Option.pair(oldParent, newParent).map(parents -> {
          var oldSubject = Subject.of(elements.left(), parents.left());
          var newSubject = Subject.of(elements.right(), parents.right());
          return UpdateAction.of(oldSubject, newSubject);
        });
      })
      .mapMulti(Option.yieldIfSome());
    return Streamer.ofCollectionWithIndex(rawActions)
      .<EditAction>mapMulti((pair, consumer) -> {
        var action = pair.left();
        var index = pair.right();
        if (replacement.isSome()) {
          var r = replacement.getOrThrow();
          var discardThreshold = r.addition().index();
          if (index < discardThreshold || action instanceof BodyUpdateAction) {
            // Discard element
            return;
          }
          if (index.equals(discardThreshold) && action instanceof AdditionAction a) {
            var oldSubject = r.deletion().action().getOldSubject();
            consumer.accept(ReplacementAction.of(oldSubject, a.getNewSubject()));
            return;
          }
        }
        if (action instanceof AdditionAction additionAction) {
          var newElement = additionAction.getNewElement();
          if (mappings.inverse().containsKey(newElement)) {
            var deletionAction = ChainMap.getOnce(mappings.inverse(), deletionsMap, newElement);
            assert deletionAction != null;
            consumer.accept(MoveAction.of(deletionAction.getOldSubject(), additionAction.getNewSubject()));
            return;
          }
        }
        consumer.accept(action);
      })
      .concat(updatesStream)
      .toList();
  }

  private Option<ReplacementTuple> findRootReplacement(Collection<EditAction> rawActions) {
    var actionsToIndices = Streamer.ofCollectionWithIndex(rawActions).collect(CollectorsX.pairsToMap());
    var candidates = Streamer.ofCollection(rawActions)
      .narrow(SimpleEditAction.class)
      .filter(a -> a.getReferenceParent() instanceof CtPackage)
      .filter(a -> a.getReferenceElement() instanceof CtType<?>)
      .toList();
    var deletion = CollectionsX.firstOfType(candidates, DeletionAction.class);
    var addition = CollectionsX.lastOfType(candidates, AdditionAction.class);
    if (deletion.isNone() || addition.isNone()) {
      return Option.None();
    }
    var deletionAction = deletion.map(d -> new ListedAction<>(d, actionsToIndices.get(d))).getOrThrow();
    var additionAction = addition.map(a -> new ListedAction<>(a, actionsToIndices.get(a))).getOrThrow();
    return Option.Some(new ReplacementTuple(deletionAction, additionAction));
  }

  public List<? extends EditAction> fromOperation(Operation<?> operation) {
    if (operation instanceof InsertOperation || operation instanceof InsertTreeOperation) {
      var node = operation.getSrcNode();
      // Result in topological order (parent nodes before children)
      return getSimpleActions(node, AdditionAction::of);
    }
    if (operation instanceof DeleteOperation || operation instanceof DeleteTreeOperation) {
      var node = operation.getSrcNode();
      // Result in reverse topological order (child nodes before parents)
      return getSimpleActions(node, DeletionAction::of).reversed();
    }
    return Collections.emptyList();
  }

  private List<? extends EditAction> getSimpleActions(CtElement changeRoot,
                                                      Function<? super Subject, ? extends EditAction> validActionMapper) {
    var nearestParent = ElementCapture.getNearestValidParent(changeRoot);
    return nearestParent
      .map(parent -> {
        // If there is a valid (captured) parent, then we try to get valid children
        // (Basically, if node is a valid child of the nearest parent, we get it and its children, ...)
        // (...otherwise we explore deeper, which can only really be the case if node is a CtBodyHolder)
        var result = ElementCapture.parseFreeElement(changeRoot, parent)
          .map(validActionMapper)
          .toList();
        return result.isEmpty() ? bodyUpdateListOf(parent) : result;
      })
      .or(() -> ElementCapture.getNearestSubjectElement(changeRoot).map(EditActions::bodyUpdateListOf))
      .getOrCompute(Collections::emptyList);
  }

  private List<BodyUpdateAction> bodyUpdateListOf(CtElement parent) {
    return List.of(bodyUpdateOf(parent));
  }

  private BodyUpdateAction bodyUpdateOf(CtElement parent) {
    return BodyUpdateAction.of(Subject.of(
      parent,
      ElementCapture.getNearestValidParent(parent).getOrThrow()
    ));
  }

  private record ListedAction<T extends EditAction>(T action, int index) {

  }

  private record ReplacementTuple(ListedAction<DeletionAction> deletion, ListedAction<AdditionAction> addition) {

  }

}
