package com.mategka.dava.analyzer.struct.property;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public sealed interface NullableProperty<T> extends TypedProperty<T> permits ParentProperty {

  default @NotNull Optional<T> asOptional() {
    return Optional.ofNullable(value());
  }

}
