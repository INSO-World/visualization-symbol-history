package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.collections.ClassSet;
import com.mategka.dava.analyzer.collections.Stack;
import com.mategka.dava.analyzer.extension.StreamsX;
import com.mategka.dava.analyzer.spoon.Spoon;

import lombok.experimental.UtilityClass;
import spoon.reflect.code.CtBodyHolder;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.declaration.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class ElementCapture {

  private final ClassSet CLASSES_TO_CAPTURE = ClassSet.of(
    CtType.class,
    CtConstructor.class,
    CtParameter.class,
    CtEnumValue.class,
    CtField.class,
    CtMethod.class,
    CtLocalVariable.class
  );
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

  public Stream<CtElement> getVariables(CtBodyHolder element) {
    if (element.getBody() == null) {
      return Stream.empty();
    }
    return element.getBody().getDirectChildren().stream()
      .flatMap(childElement -> {
        if (childElement instanceof CtType<?> || childElement instanceof CtLambda<?>) {
          return Stream.empty();
        }
        if (childElement instanceof CtBodyHolder bodyHolder) {
          return getVariables(bodyHolder);
        }
        if (childElement instanceof CtLocalVariable<?> localVariable) {
          return Stream.of(localVariable);
        }
        return Stream.empty();
      });
  }

  public Stream<? extends CtElement> relevantChildrenOf(CtElement element) {
    if (element instanceof CtType<?> type) {
      return type.getTypeMembers().stream();
    } else if (element instanceof CtConstructor<?> constructor) {
      if (Spoon.isRegularConstructor(constructor)) {
        return Stream.concat(constructor.getParameters().stream(), getVariables(constructor));
      }
    } else if (element instanceof CtMethod<?> method) {
      return Stream.concat(method.getParameters().stream(), getVariables(method));
    }
    return Stream.empty();
  }

  public Stream<Subject> parseElement(CtElement element, CtElement parent) {
    if (!CLASSES_TO_CAPTURE.containsClassOf(element)) {
      return Stream.empty();
    }
    if (element instanceof CtConstructor<?> constructor && !Spoon.isRegularConstructor(constructor)) {
      return Stream.empty();
    }
    element.setAllMetadata(Map.of("%captured%", true));
    return StreamsX.cons(
      Subject.of(element, parent),
      relevantChildrenOf(element).flatMap(m -> parseElement(m, element))
    );
  }

  public Stream<Subject> parseFreeElement(CtElement element, CtElement parent) {
    if (parent instanceof CtPackage || parent instanceof CtType<?>) {
      return parseElement(element, parent);
    } else if (parent instanceof CtConstructor<?> || parent instanceof CtMethod<?>) {
      if (element instanceof CtParameter<?> || element instanceof CtLocalVariable<?>) {
        return parseElement(element, parent);
      }
      if (element instanceof CtBodyHolder bodyHolder) {
        return getVariables(bodyHolder).flatMap(e -> parseElement(e, parent));
      }
      return Stream.empty();
    }
    throw new IllegalArgumentException("Given parent constitutes an illegal symbol parent");
  }

  public Optional<CtElement> getNearestSubjectElement(CtElement node) {
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
      var relevantChildren = relevantChildrenOf(current).collect(Collectors.toSet());
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

  public Optional<CtElement> getNearestValidParent(CtElement node) {
    CtElement current = node;
    Queue<CtElement> parents = new ArrayDeque<>();
    boolean expectingType = VALID_TYPE_MEMBER_NODE_CLASSES.containsClassOf(current);
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

}
