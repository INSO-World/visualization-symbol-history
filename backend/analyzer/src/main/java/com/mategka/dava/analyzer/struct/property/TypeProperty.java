package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.property.value.type.Type;

@PropertyKey("type")
public record TypeProperty(Type value) implements SimpleProperty<Type> {

  @Override
  public String toString() {
    return value.toString();
  }

}
