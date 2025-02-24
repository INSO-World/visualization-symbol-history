package com.mategka.dava.analyzer.struct.property.value;

import spoon.reflect.code.CtExpression;

public record Expression(String string) {

  public static Expression fromSpoon(CtExpression<?> expression) {
    return new Expression(expression.toString());
  }

  @Override
  public String toString() {
    return string;
  }

}
