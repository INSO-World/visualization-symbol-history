package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;

@PropertyKey("kind")
public record KindProperty(Value value) implements EnumProperty<KindProperty.Value> {

  public enum Value {
    PACKAGE,
    CLASS,
    INTERFACE,
    ENUM,
    FIELD,
    METHOD,
    VARIABLE,
    PARAMETER,
  }

}
