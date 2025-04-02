package com.mategka.dava.analyzer.extension.traitlike;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

public interface ParameterizedIterable<T, P> extends Iterable<T> {

  @Override
  default @NotNull Iterator<T> iterator() {
    return iterator(null);
  }

  @NotNull Iterator<T> iterator(@Nullable P parameter);

}
