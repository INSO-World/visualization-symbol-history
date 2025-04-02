package com.mategka.dava.analyzer.extension.traitlike;

import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public interface Streamable<T, S extends Stream<T>> {

  @NotNull S stream();

  default @NotNull S parallelStream() {
    //noinspection unchecked
    return (S) stream().parallel();
  }

}
