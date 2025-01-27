package com.mategka.dava.analyzer.struct.property;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public non-sealed interface OptionalProperty<T> extends TypedProperty<Optional<? extends T>> {

  @NotNull Optional<? extends T> value();

}
