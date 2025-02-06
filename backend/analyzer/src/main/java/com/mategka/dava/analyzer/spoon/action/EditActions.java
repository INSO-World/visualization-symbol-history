package com.mategka.dava.analyzer.spoon.action;

import com.mategka.dava.analyzer.collections.ClassSet;
import com.mategka.dava.analyzer.collections.Stack;
import com.mategka.dava.analyzer.spoon.Spoon;
import com.mategka.dava.analyzer.struct.symbol.ElementCapture;
import com.mategka.dava.analyzer.struct.symbol.Subject;

import gumtree.spoon.diff.operations.*;
import lombok.experimental.UtilityClass;
import spoon.reflect.code.CtBodyHolder;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.declaration.*;

import java.util.*;
import java.util.stream.Collectors;

@UtilityClass
public class EditActions {

  private final ClassSet INVALID_PARENT_NODE_CLASSES = ClassSet.of(
    CtField.class,
    CtLocalVariable.class,
    CtParameter.class,
    CtLambda.class
  );

  private final ClassSet VALID_TYPE_MEMBER_NODE_CLASSES = ClassSet.of(
    CtMethod.class,
    CtConstructor.class,
    CtType.class
  );

  private final ClassSet VALID_PARENT_NODE_CLASSES = ClassSet.of(
    CtPackage.class,
    CtType.class,
    CtMethod.class,
    CtConstructor.class
  );

  public List<? extends EditAction> fromOperation(Operation<?> operation) {
    if (operation instanceof InsertOperation || operation instanceof InsertTreeOperation) {
      var node = operation.getSrcNode();
      var nearestParent = getNearestValidParent(node);
      return nearestParent
        .map(parent -> {
          // If there is a valid (captured) parent, then we try to get valid children
          // (Basically, if node is a valid child of the nearest parent, we get it and its children, ...)
          // (...otherwise we explore deeper, which can only really be the case if node is a CtBodyHolder)
          var result = ElementCapture.parseFreeElement(node, parent)
            .map(AdditionAction::of)
            .toList();
          return result.isEmpty() ? deepUpdateListOf(parent) : result;
        })
        .or(() -> getNearestSubjectElement(node).map(EditActions::deepUpdateListOf))
        .orElseGet(Collections::emptyList);
    }
    if (operation instanceof DeleteOperation || operation instanceof DeleteTreeOperation) {
      var node = operation.getSrcNode();
      var nearestParent = getNearestValidParent(node);
      return nearestParent
        .map(parent -> {
          var result = ElementCapture.parseFreeElement(node, parent)
            .map(DeletionAction::of)
            .toList()
            .reversed();
          return result.isEmpty() ? deepUpdateListOf(parent) : result;
        })
        .or(() -> getNearestSubjectElement(node).map(EditActions::deepUpdateListOf))
        .orElseGet(Collections::emptyList);
    }
    if (operation instanceof MoveOperation) {
      // TODO
      return Collections.emptyList();
    }
    if (operation instanceof UpdateOperation) {
      // TODO
      return Collections.emptyList();
    }
    return Collections.emptyList();
  }

  private Optional<CtElement> getNearestSubjectElement(CtElement node) {
    CtElement current = node;
    Stack<CtElement> parents = new Stack<>();
    do {
      if (!current.isParentInitialized()) {
        // No parent available, abort (should not occur since node must not be at package level or above)
        return Optional.empty();
      }
      current = current.getParent();
      if (current == null) {
        // Invalid parent, abort
        return Optional.empty();
      }
      parents.push(current);
    } while (!(current instanceof CtPackage));
    current = parents.pop();
    while (!parents.isEmpty()) {
      var next = parents.pop();
      var relevantChildren = ElementCapture.relevantChildrenOf(current).collect(Collectors.toSet());
      if (relevantChildren.stream().noneMatch(next::equals)) {
        // If the next parent is not part of the relevant children of the parent's parent, then we found the LCA
        if (current instanceof CtExecutable<?> && next instanceof CtBodyHolder && !(next instanceof CtLambda<?>)) {
          // However, if we have a CtExecutable-to-CtBodyHolder parent sequence, a variable declaration may be the LCA
          if (relevantChildren.contains(node)) {
            // If the node itself IS one of these variable declarations, then its parent is the previous parent
            return Optional.of(current);
          }
          // Otherwise, the declaration among the relevantChildren would have to be among the remaining parents
          var variableDeclaration = parents.stream().filter(relevantChildren::contains).findFirst();
          if (variableDeclaration.isPresent()) {
            // If it is, then that is the correct parent (and node is a child of it)
            return variableDeclaration;
          }
          // Otherwise, we fall back to the default behavior if a parent sequence disparity is found
        }
        return Optional.of(current);
      }
      current = next;
    }
    return Optional.empty();
  }

  private Optional<CtElement> getNearestValidParent(CtElement node) {
    CtElement current = node;
    Queue<CtElement> parents = new ArrayDeque<>();
    boolean expectingType = false;
    while (true) {
      if (!current.isParentInitialized()) {
        // No parent available (should not occur since node must not be at package level or above)
        return Optional.empty();
      }
      current = current.getParent();
      if (current == null) {
        // Invalid parent, abort
        return Optional.empty();
      }
      parents.add(current);
      if (current instanceof CtPackage) {
        // Previous type was main type of .java file, all parents collected
        break;
      }
      if (expectingType && !(current instanceof CtType<?>)) {
        // Was expecting type but parent was not, abort
        return Optional.empty();
      }
      if (INVALID_PARENT_NODE_CLASSES.containsClassOf(current)) {
        // Invalid parent type, abort (symbols under these are never added)
        return Optional.empty();
      }
      if (current instanceof CtConstructor<?> constructor && !Spoon.isRegularConstructor(constructor)) {
        // Invalid parent type, abort (only symbols under explicit, regular constructors are ever captured)
        return Optional.empty();
      }
      if (VALID_TYPE_MEMBER_NODE_CLASSES.containsClassOf(current)) {
        // Expect main type or type declared in main type (e.g., not an anonymous class)
        expectingType = true;
      }
    }
    return parents.stream()
      .filter(VALID_PARENT_NODE_CLASSES::containsClassOf)
      .findFirst();
  }

  private List<DeepUpdateAction> deepUpdateListOf(CtElement parent) {
    return List.of(deepUpdateOf(parent));
  }

  private DeepUpdateAction deepUpdateOf(CtElement parent) {
    return DeepUpdateAction.of(Subject.of(
      parent,
      getNearestValidParent(parent).orElseThrow()
    ));
  }

}
