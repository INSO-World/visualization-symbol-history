package com.mategka.dava.analyzer.struct.property;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public sealed interface ListProperty<T> extends TypedProperty<List<? extends T>> permits AnnotationsProperty {

  @NotNull List<? extends T> value();

}
