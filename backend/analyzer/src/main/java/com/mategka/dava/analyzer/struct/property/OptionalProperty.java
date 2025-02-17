package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.extension.option.Option;

import org.jetbrains.annotations.NotNull;

public non-sealed interface OptionalProperty<T> extends TypedProperty<Option<? extends T>> {

  @NotNull Option<? extends T> value();

}
