package com.mategka.dava.analyzer.struct.property;

import org.jetbrains.annotations.NotNull;

public sealed interface SimpleProperty<T> extends TypedProperty<T>
  permits EnumProperty, LineRangeProperty, PathProperty, SimpleNameProperty, TypeProperty, VisibilityProperty {

  @NotNull T value();

}
