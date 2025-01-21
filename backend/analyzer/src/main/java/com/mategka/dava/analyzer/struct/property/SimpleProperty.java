package com.mategka.dava.analyzer.struct.property;

import org.jetbrains.annotations.NotNull;

public sealed interface SimpleProperty<T> extends Property
  permits EnumProperty, LineNumberProperty, ParentProperty, SimpleNameProperty, TypeProperty {

  @NotNull T value();

}
