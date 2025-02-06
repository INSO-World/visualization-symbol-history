package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.collections.ClassSet;
import com.mategka.dava.analyzer.extension.StreamsX;
import com.mategka.dava.analyzer.spoon.Spoon;

import lombok.experimental.UtilityClass;
import spoon.reflect.code.CtBodyHolder;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.declaration.*;

import java.util.Map;
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

}
