package com.mategka.dava.analyzer.struct.property;

import org.jetbrains.annotations.NotNull;

public sealed interface SimpleProperty<T> extends TypedProperty<T>
  permits AnalyzerLevelProperty, BodyHashProperty, CtPathProperty, EnumProperty, InitialValueProperty, LineRangeProperty, PathProperty, SimpleNameProperty, SpoonPathProperty, TypeProperty, VisibilityProperty {

  @NotNull T value();

}
