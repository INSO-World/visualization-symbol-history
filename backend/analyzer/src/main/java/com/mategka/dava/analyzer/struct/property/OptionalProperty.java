package com.mategka.dava.analyzer.struct.property;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public sealed interface OptionalProperty<T> extends Property permits ParentProperty {

  @NotNull Optional<T> value();

}
