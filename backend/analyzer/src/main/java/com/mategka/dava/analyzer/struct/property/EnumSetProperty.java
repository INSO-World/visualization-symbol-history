package com.mategka.dava.analyzer.struct.property;

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public sealed interface EnumSetProperty<T extends Enum<T>> extends SetProperty<T>
  permits ModifiersProperty, VisibilityProperty {

  @Override
  @NotNull EnumSet<T> value();

}
