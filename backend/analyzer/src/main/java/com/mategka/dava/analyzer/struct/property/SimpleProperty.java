package com.mategka.dava.analyzer.struct.property;

import org.jetbrains.annotations.NotNull;

public sealed interface SimpleProperty<T> extends Property
  permits EnumProperty, LineRangeProperty, SimpleNameProperty, PathProperty, TypeProperty, VisibilityProperty {

  @NotNull T value();

}
