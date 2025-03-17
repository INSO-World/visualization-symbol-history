package com.mategka.dava.analyzer.spoon.action;

import com.mategka.dava.analyzer.collections.ChainMap;
import com.mategka.dava.analyzer.extension.*;
import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.struct.pipeline.ElementCapture;
import com.mategka.dava.analyzer.struct.symbol.Subject;

import com.google.common.collect.BiMap;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.*;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtElement;

import java.util.*;
import java.util.function.BiFunction;
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
        .flatMap(Collection::stream)
        .distinct()
        .toList();
    var replacement = ReplacementTuple.find(rawActions);
    if (replacement.isSome()) {
      // Remove possible (now invalidated) mapping for the root replacement nodes
      var r = replacement.getOrThrow();
      var addedElement = r.addition().action().getNewElement();
      var removedAdditionElement = mappings.remove(r.deletion().action().getOldElement());
      // Special case if a mapping FROM the old root node exists, but it is not TO the new root node
      if (removedAdditionElement != addedElement) {
        mappings.inverse().remove(addedElement);
      }
    }
    var mappedDeletionsMap = AnStream.from(rawActions)
        .allow(DeletionAction.class)
        .map(Pair.fromRight(DeletionAction::getOldElement))
        .filter(p -> mappings.containsKey(p.left()))
        .collect(CollectorsX.pairsToMap());
    var updatesStream = getUpdatesStream(mappings, mappedDeletionsMap.keySet());
    return replacement.fold(
        r -> editsWithReplacement(rawActions, r, mappings, mappedDeletionsMap),
        () -> editsWithoutReplacement(rawActions, mappings, mappedDeletionsMap)
      )
      .concat(updatesStream)
      .toList();
  }

  private List<SimpleEditAction> bodyUpdateListOf(CtElement parent) {
    return List.of(bodyUpdateOf(parent));
  }

  private BodyUpdateAction bodyUpdateOf(CtElement parent) {
    return BodyUpdateAction.of(Subject.of(
      parent,
      ElementCapture.getNearestValidParent(parent).getOrThrow()
    ));
  }

  private AnStream<EditAction> editsWithReplacement(List<SimpleEditAction> rawActions, ReplacementTuple replacement,
                                                    BiMap<CtElement, CtElement> mappings,
                                                    Map<CtElement, DeletionAction> mappedDeletionsMap) {
    var replacementStream = AnStream.<EditAction>from(ReplacementAction.fromTuple(replacement));
    Set<CtElement> mappedParents = new HashSet<>();
    mappedParents.add(replacement.addition().action().getNewElement());
    var mainStream = AnStream.from(rawActions)
        .skip(replacement.addition().index() + 1)
        .reject(BodyUpdateAction.class)
        .map((action) -> {
          if (action instanceof AdditionAction additionAction) {
            var newElement = additionAction.getNewElement();
            if (mappings.inverse().containsKey(newElement)) {
              var deletionAction = MapsX.get(mappings.inverse(), mappedDeletionsMap, newElement);
              assert deletionAction != null;
              BiFunction<Subject, Subject, EditAction> actionConstructor = MoveAction::of;
              if (mappedParents.contains(additionAction.getNewParent())) {
                mappedParents.add(newElement);
                actionConstructor = UpdateAction::of;
              }
              return actionConstructor.apply(deletionAction.getOldSubject(), additionAction.getNewSubject());
            }
          }
          return action;
        });
    var deletionsStream = AnStream.from(rawActions)
        .limit(replacement.deletion().index())
        .allow(DeletionAction.class)
        .filter(deletion -> !mappedDeletionsMap.containsKey(deletion.getOldElement()));
    // NOTE: This is safe since unchanged symbols cannot have symbols as parents that have been deleted
    return replacementStream.concat(mainStream).concat(deletionsStream);
  }

  private AnStream<EditAction> editsWithoutReplacement(List<SimpleEditAction> rawActions,
                                                       BiMap<CtElement, CtElement> mappings,
                                                       Map<CtElement, DeletionAction> mappedDeletionsMap) {
    return AnStream.from(rawActions)
        .map((action) -> {
          if (action instanceof AdditionAction additionAction) {
            var newElement = additionAction.getNewElement();
            if (mappings.inverse().containsKey(newElement)) {
              var deletionAction = MapsX.get(mappings.inverse(), mappedDeletionsMap, newElement);
              assert deletionAction != null;
              return MoveAction.of(deletionAction.getOldSubject(), additionAction.getNewSubject());
            }
          }
          return action;
        });
  }

  private List<SimpleEditAction> fromOperation(Operation<?> operation) {
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

  private List<SimpleEditAction> getSimpleActions(CtElement changeRoot,
                                                  Function<? super Subject, SimpleEditAction> validActionMapper) {
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

  private Stream<UpdateAction> getUpdatesStream(BiMap<CtElement, CtElement> mappings, Set<CtElement> deletions) {
    var updateMappingKeys = SetsX.difference(mappings.keySet(), deletions);
    return AnStream.from(mappings.entrySet())
      .map(Pair::fromEntry)
      .filter(Pair.filteringLeft(updateMappingKeys::contains))
      .filter(Pair.filtering(ElementCapture.CLASSES_TO_CAPTURE::containsClassOf))
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

}
