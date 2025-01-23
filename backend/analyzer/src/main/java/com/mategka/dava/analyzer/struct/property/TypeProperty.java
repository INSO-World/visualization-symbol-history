package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import spoon.reflect.declaration.CtTypedElement;

@PropertyKey("type")
public record TypeProperty(TypeValue value) implements SimpleProperty<TypeValue> {

  public static TypeProperty unknownFromTypedElement(CtTypedElement<?> typedElement) {
    return new TypeProperty(TypeValue.UnknownType.of(typedElement.getType().getQualifiedName()));
  }

  @Override
  public String toString() {
    return value.toString();
  }

}
