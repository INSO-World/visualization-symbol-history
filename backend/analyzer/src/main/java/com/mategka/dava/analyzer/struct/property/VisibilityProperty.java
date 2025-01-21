package com.mategka.dava.analyzer.struct.property;

import java.util.EnumSet;

public record VisibilityProperty(EnumSet<Visibility> value) implements EnumSetProperty<VisibilityProperty.Visibility> {

  public enum Visibility {
    PUBLIC,
    PROTECTED,
    PRIVATE,
    PACKAGE_PRIVATE,
  }

}
