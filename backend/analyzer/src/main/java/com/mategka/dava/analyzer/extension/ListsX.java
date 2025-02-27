package com.mategka.dava.analyzer.extension;

import com.mategka.dava.analyzer.extension.stream.AnStream;

import com.leakyabstractions.result.core.Results;
import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

@UtilityClass
public class ListsX {

  @SafeVarargs
  public <T> List<T> cons(T head, List<T>... tails) {
    return Stream.concat(
      Stream.of(head),
      Arrays.stream(tails).flatMap(Collection::stream)
    ).toList();
  }

  public <T, U> List<U> flatMap(List<T> list, Function<? super T, List<U>> mapper) {
    if (list == null) {
      return null;
    }
    return list.stream().map(mapper).flatMap(Collection::stream).toList();
  }

  public boolean isImmutable(List<?> list) {
    return Results.ofCallable(() -> list.addAll(Collections.emptyList()))
      .getFailure()
      .map(e -> e instanceof UnsupportedOperationException)
      .orElse(false);
  }

  public <T, U> List<U> map(Collection<T> list, Function<? super T, U> mapper) {
    if (list == null) {
      return null;
    }
    return list.stream().map(mapper).toList();
  }

  public <T, U extends T> List<U> sublistOfType(Collection<T> collection, Class<U> clazz) {
    return AnStream.from(collection).filter(clazz).toList();
  }

}
