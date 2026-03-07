package com.mategka.dava.analyzer.struct.property;

import org.jetbrains.annotations.NotNull;

public sealed interface StringProperty extends SimpleProperty<String>
  permits PathProperty, SimpleNameProperty, SpoonPathProperty {

  @NotNull String value();

}
