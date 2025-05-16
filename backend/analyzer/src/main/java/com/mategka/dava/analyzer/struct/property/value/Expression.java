package com.mategka.dava.analyzer.struct.property.value;

import com.fasterxml.jackson.annotation.JsonValue;
import spoon.reflect.code.CtExpression;

import java.io.Serial;
import java.io.Serializable;

public record Expression(@JsonValue String string) implements Serializable {

  @Serial
  private static final long serialVersionUID = 3684914665069310838L;

  public static Expression fromSpoon(CtExpression<?> expression) {
    return new Expression(expression.toString());
  }

  @Override
  public String toString() {
    return string;
  }

}
