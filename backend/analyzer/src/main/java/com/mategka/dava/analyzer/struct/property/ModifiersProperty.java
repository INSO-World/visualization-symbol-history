package com.mategka.dava.analyzer.struct.property;

import java.util.EnumSet;

public record ModifiersProperty(EnumSet<Modifier> value) implements EnumSetProperty<ModifiersProperty.Modifier> {

  public enum Modifier {
    STATIC,
    SEALED,
    ABSTRACT,
    FINAL,
    SYNCHRONIZED,
    TRANSIENT,
    VOLATILE,
  }

}
