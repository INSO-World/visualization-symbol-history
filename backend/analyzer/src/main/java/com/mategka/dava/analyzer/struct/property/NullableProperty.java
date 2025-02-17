package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.extension.option.Option;

import org.jetbrains.annotations.NotNull;

public sealed interface NullableProperty<T> extends TypedProperty<T> permits ParentProperty {

  default @NotNull Option<T> asOption() {
    return Option.fromNullable(value());
  }

}
