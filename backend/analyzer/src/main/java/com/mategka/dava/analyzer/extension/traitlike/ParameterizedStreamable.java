package com.mategka.dava.analyzer.extension.traitlike;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public interface ParameterizedStreamable<T, S extends Stream<T>, P> extends Streamable<T, S> {

  @Override
  default @NotNull S stream() {
    return stream(null);
  }

  @NotNull S stream(@Nullable P parameter);

  @Override
  default @NotNull S parallelStream() {
    return parallelStream(null);
  }

  default @NotNull S parallelStream(@Nullable P parameter) {
    //noinspection unchecked
    return (S) stream(parameter).parallel();
  }

}
