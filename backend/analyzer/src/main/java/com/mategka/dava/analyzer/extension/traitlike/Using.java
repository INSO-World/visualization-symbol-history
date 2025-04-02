package com.mategka.dava.analyzer.extension.traitlike;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

@UtilityClass
public class Using {

  public <T, P> Iterable<T> iterator(ParameterizedIterable<T, P> iterable, P parameter) {
    return () -> iterable.iterator(parameter);
  }

  public <T, S extends Stream<T>, P> Streamable<T, S> stream(ParameterizedStreamable<T, S, P> streamable, P parameter) {
    return new Streamable<>() {

      @Override
      public @NotNull S parallelStream() {
        return streamable.parallelStream(parameter);
      }

      @Override
      public @NotNull S stream() {
        return streamable.stream(parameter);
      }

    };
  }

}
