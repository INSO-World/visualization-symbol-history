package com.mategka.dava.analyzer.struct.property;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public non-sealed interface OptionalProperty<T> extends Property {

  @NotNull Optional<T> value();

}
