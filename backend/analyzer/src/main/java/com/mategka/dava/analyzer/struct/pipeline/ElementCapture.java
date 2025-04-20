package com.mategka.dava.analyzer.struct.pipeline;

import com.mategka.dava.analyzer.collections.ClassSet;
import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.spoon.Spoon;
import com.mategka.dava.analyzer.struct.symbol.Subject;

import lombok.experimental.UtilityClass;
import spoon.reflect.code.CtBodyHolder;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.declaration.*;

import java.util.Map;

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

  public AnStream<Subject> parseElement(CtElement element, CtElement parent) {
    if (!CLASSES_TO_CAPTURE.containsClassOf(element)) {
      return AnStream.empty();
    }
    if (element instanceof CtConstructor<?> constructor && !Spoon.isRegularConstructor(constructor)) {
      return AnStream.empty();
    }
    element.setAllMetadata(Map.of("%captured%", true));
    return AnStream.cons(
      Subject.of(element, parent),
      relevantChildrenOf(element).flatMap(m -> parseElement(m, element))
    );
  }

  private AnStream<CtElement> getVariables(CtBodyHolder element) {
    if (element.getBody() == null) {
      return AnStream.empty();
    }
    return AnStream.from(element.getBody().getDirectChildren())
      .flatMap(childElement -> {
        if (childElement instanceof CtType<?> || childElement instanceof CtLambda<?>) {
          return AnStream.empty();
        }
        if (childElement instanceof CtBodyHolder bodyHolder) {
          return getVariables(bodyHolder);
        }
        if (childElement instanceof CtLocalVariable<?> localVariable) {
          return AnStream.singleton(localVariable);
        }
        return AnStream.empty();
      });
  }

  private AnStream<? extends CtElement> relevantChildrenOf(CtElement element) {
    if (element instanceof CtType<?> type) {
      return AnStream.from(type.getTypeMembers());
    } else if (element instanceof CtConstructor<?> constructor) {
      if (Spoon.isRegularConstructor(constructor)) {
        return AnStream.<CtElement>from(constructor.getParameters()).concat(getVariables(constructor));
      }
    } else if (element instanceof CtMethod<?> method) {
      return AnStream.<CtElement>from(method.getParameters()).concat(getVariables(method));
    }
    return AnStream.empty();
  }

}
