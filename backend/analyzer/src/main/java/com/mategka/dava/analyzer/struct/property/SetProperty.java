package com.mategka.dava.analyzer.struct.property;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

public non-sealed interface SetProperty<T> extends Property {

  @NotNull Set<T> value();

}
