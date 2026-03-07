package com.mategka.dava.analyzer.spoon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.reference.CtTypeReference;

public class CtCaseHotfixProcessor extends AbstractProcessor<CtCase<?>> {

  @Override
  public boolean isToBeProcessed(CtCase<?> candidate) {
    return candidate.getCaseExpression() instanceof CtFieldAccess<?> fieldAccess && fieldAccess.getTarget() == null;
  }

  @Override
  public void process(@NotNull CtCase<?> element) {
    CtFieldAccess<?> fieldAccess = (CtFieldAccess<?>) element.getCaseExpression();
    @Nullable CtTypeReference<?> declaringType = fieldAccess.getVariable().getDeclaringType();
    CtExpression<?> typeAccess = getFactory().createTypeAccess(declaringType);
    typeAccess.setImplicit(true);
    fieldAccess.setTarget(typeAccess);
  }

}
