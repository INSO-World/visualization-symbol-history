package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.option.Options;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public sealed interface NullableProperty<T extends Serializable> extends SerializableProperty<T> permits ParentProperty {

  default @NotNull Option<T> asOption() {
    return Options.fromNullable(value());
  }

}
