package com.mategka.dava.analyzer.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@UtilityClass
public class Lists {

  @SafeVarargs
  public <T> @NotNull List<T> mutableOf(T @NotNull ... elements) {
    return new ArrayList<>(Arrays.asList(elements));
  }

  public <T, U> List<U> map(List<T> list, Function<T, U> mapper) {
    if (list == null) {
      return null;
    }
    return list.stream().map(mapper).toList();
  }

  public <T, U> List<U> flatMap(List<T> list, Function<T, List<U>> mapper) {
    if (list == null) {
      return null;
    }
    return list.stream().map(mapper).flatMap(Collection::stream).toList();
  }

  @SafeVarargs
  public <T> List<T> cons(T head, List<T>... tails) {
    return Stream.concat(
      Stream.of(head),
      Arrays.stream(tails).flatMap(Collection::stream)
    ).toList();
  }

}
