package com.mategka.dava.analyzer.struct.property;

import org.jetbrains.annotations.NotNull;

public sealed interface SimpleProperty<T> extends TypedProperty<T>
  permits AnalyzerLevelProperty, BodyHashProperty, EnumProperty, InitialValueProperty, LineRangeProperty, CtPathProperty, PathProperty, SimpleNameProperty, TypeProperty, VisibilityProperty {

  @NotNull T value();

}
