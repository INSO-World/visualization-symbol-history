package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.collections.ClassSet;
import com.mategka.dava.analyzer.collections.Stack;
import com.mategka.dava.analyzer.extension.MyStream;
import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.spoon.Spoon;

import lombok.experimental.UtilityClass;
import spoon.reflect.code.CtBodyHolder;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.declaration.*;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class ElementCapture {

  public final ClassSet CLASSES_TO_CAPTURE = ClassSet.of(
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

  public MyStream<CtElement> getVariables(CtBodyHolder element) {
    if (element.getBody() == null) {
      return MyStream.empty();
    }
    return MyStream.from(element.getBody().getDirectChildren())
      .flatMap(childElement -> {
        if (childElement instanceof CtType<?> || childElement instanceof CtLambda<?>) {
          return MyStream.empty();
        }
        if (childElement instanceof CtBodyHolder bodyHolder) {
          return getVariables(bodyHolder);
        }
        if (childElement instanceof CtLocalVariable<?> localVariable) {
          return MyStream.from(localVariable);
        }
        return MyStream.empty();
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

  public MyStream<Subject> parseElement(CtElement element, CtElement parent) {
    if (!CLASSES_TO_CAPTURE.containsClassOf(element)) {
      return MyStream.empty();
    }
    if (element instanceof CtConstructor<?> constructor && !Spoon.isRegularConstructor(constructor)) {
      return MyStream.empty();
    }
    element.setAllMetadata(Map.of("%captured%", true));
    return MyStream.cons(
      Subject.of(element, parent),
      relevantChildrenOf(element).flatMap(m -> parseElement(m, element))
    );
  }

  public MyStream<Subject> parseFreeElement(CtElement element, CtElement parent) {
    if (parent instanceof CtPackage || parent instanceof CtType<?>) {
      return parseElement(element, parent);
    } else if (parent instanceof CtConstructor<?> || parent instanceof CtMethod<?>) {
      if (element instanceof CtParameter<?> || element instanceof CtLocalVariable<?>) {
        return parseElement(element, parent);
      }
      if (element instanceof CtBodyHolder bodyHolder) {
        return getVariables(bodyHolder).flatMap(e -> parseElement(e, parent));
      }
      return MyStream.empty();
    }
    throw new IllegalArgumentException("Given parent constitutes an illegal symbol parent");
  }

  public Option<CtElement> getNearestSubjectElement(CtElement node) {
    CtElement current = node;
    Stack<CtElement> parents = new Stack<>();
    do {
      if (!current.isParentInitialized()) {
        // No parent available, abort (should not occur since node must not be at package level or above)
        return Option.None();
      }
      current = current.getParent();
      if (current == null) {
        // Invalid parent, abort
        return Option.None();
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
            return Option.Some(current);
          }
          // Otherwise, the declaration among the relevantChildren would have to be among the remaining parents
          var variableDeclaration = MyStream.from(parents)
            .filter(relevantChildren::contains)
            .findFirstAsOption();
          if (variableDeclaration.isSome()) {
            // If it is, then that is the correct parent (and node is a child of it)
            return variableDeclaration;
          }
          // Otherwise, we fall back to the default behavior if a parent sequence disparity is found
        }
        return Option.Some(current);
      }
      current = next;
    }
    return Option.None();
  }

  public Option<CtElement> getNearestValidParent(CtElement node) {
    CtElement current = node;
    Queue<CtElement> parents = new ArrayDeque<>();
    boolean expectingType = VALID_TYPE_MEMBER_NODE_CLASSES.containsClassOf(current);
    while (true) {
      if (!current.isParentInitialized()) {
        // No parent available (should not occur since node must not be at package level or above)
        return Option.None();
      }
      current = current.getParent();
      if (current == null) {
        // Invalid parent, abort
        return Option.None();
      }
      parents.add(current);
      if (current instanceof CtPackage) {
        // Previous type was main type of .java file, all parents collected
        break;
      }
      if (expectingType && !(current instanceof CtType<?>)) {
        // Was expecting type but parent was not, abort
        return Option.None();
      }
      if (INVALID_PARENT_NODE_CLASSES.containsClassOf(current)) {
        // Invalid parent type, abort (symbols under these are never added)
        return Option.None();
      }
      if (current instanceof CtConstructor<?> constructor && !Spoon.isRegularConstructor(constructor)) {
        // Invalid parent type, abort (only symbols under explicit, regular constructors are ever captured)
        return Option.None();
      }
      if (VALID_TYPE_MEMBER_NODE_CLASSES.containsClassOf(current)) {
        // Expect main type or type declared in main type (e.g., not an anonymous class)
        expectingType = true;
      }
    }
    return MyStream.from(parents)
      .filter(VALID_PARENT_NODE_CLASSES::containsClassOf)
      .findFirstAsOption();
  }

}
