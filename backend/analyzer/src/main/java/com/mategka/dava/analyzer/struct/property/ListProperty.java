package com.mategka.dava.analyzer.struct.property;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public non-sealed interface ListProperty<T> extends Property {

  @NotNull List<T> value();

}
