package com.mategka.dava.analyzer.struct.property;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public sealed interface SimpleProperty<T extends Serializable> extends SerializableProperty<T>
  permits AnalyzerLevelProperty, BodyHashProperty, InitialValueProperty, LineRangeProperty, StringProperty, TypeProperty {

  @NotNull T value();

}
