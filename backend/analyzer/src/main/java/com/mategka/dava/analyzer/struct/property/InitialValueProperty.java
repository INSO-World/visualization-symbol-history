package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.property.value.Expression;

@PropertyKey("initialValue")
public record InitialValueProperty(Expression value) implements SimpleProperty<Expression> {

  @Override
  public String toString() {
    return value.toString();
  }

}
