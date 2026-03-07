package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.property.value.Kind;

import spoon.reflect.declaration.CtType;

@PropertyKey("kind")
public record KindProperty(Kind value) implements EnumProperty<Kind> {

  public static KindProperty fromType(CtType<?> type) {
    return Kind.fromType(type).toProperty();
  }

  @Override
  public String toString() {
    return value.toPseudoKeyword();
  }

}
