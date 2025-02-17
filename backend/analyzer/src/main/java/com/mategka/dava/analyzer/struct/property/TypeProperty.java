package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.property.value.Type;
import com.mategka.dava.analyzer.struct.property.value.UnknownType;

import spoon.reflect.declaration.CtTypedElement;

@PropertyKey("type")
public record TypeProperty(Type value) implements SimpleProperty<Type> {

  public static TypeProperty unknownFromTypedElement(CtTypedElement<?> typedElement) {
    return new TypeProperty(UnknownType.of(typedElement.getType().getQualifiedName()));
  }

  @Override
  public String toString() {
    return value.toString();
  }

}
