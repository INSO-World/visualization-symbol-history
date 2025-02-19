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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

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
    var deletionsMap = MyStream.from(rawActions)
      .narrow(DeletionAction.class)
      .map(Pair.fromRight(DeletionAction::getOldElement))
      .filter(p -> mappings.containsKey(p.left()))
      .collect(CollectorsX.pairsToMap());
    var replacement = ReplacementTuple.find(rawActions);
    var updatesStream = getUpdatesStream(mappings, deletionsMap.keySet());
    return MyStream.fromIndexed(rawActions)
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
          if (index.equals(discardThreshold) && action instanceof AdditionAction) {
            consumer.accept(ReplacementAction.fromTuple(r));
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

  private Stream<UpdateAction> getUpdatesStream(BiMap<CtElement, CtElement> mappings, Set<CtElement> deletions) {
    var updateMappingKeys = SetsX.difference(mappings.keySet(), deletions);
    return PairStream.mapping(mappings.entrySet(), Pair::fromEntry)
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
  }

  private List<? extends EditAction> fromOperation(Operation<?> operation) {
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

}
