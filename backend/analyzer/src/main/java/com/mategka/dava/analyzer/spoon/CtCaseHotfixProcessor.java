package com.mategka.dava.analyzer.spoon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spoon.processing.AbstractProcessor;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.List;

public class CtCaseHotfixProcessor extends AbstractProcessor<CtCase<?>> {

  @Override
  public void process(@NotNull CtCase<?> element) {
    CtExpression<?> caseExpression = element.getCaseExpression();
    if (caseExpression instanceof CtFieldAccess<?> fieldAccess) {
      if (fieldAccess.getTarget() == null) {
        Factory factory = getFactory();
        @Nullable CtTypeReference<?> declaringType = fieldAccess.getVariable().getDeclaringType();
        CtExpression<?> typeAccess = factory.createTypeAccess(declaringType);
        typeAccess.setImplicit(true);
        fieldAccess.setTarget(typeAccess);
      }
    }
  }

  public void applyTo(@NotNull CtModel model) {
    List<CtCase<?>> cases = model.getElements(new TypeFilter<>(CtCase.class));
    if (!cases.isEmpty()) {
      setFactory(cases.getFirst().getFactory());
      for (CtCase<?> ctCase : cases) {
        process(ctCase);
      }
    }
  }

}
