package com.mategka.dava.analyzer.extension;

import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.stream.AnStream;

import com.leakyabstractions.result.core.Results;
import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

@UtilityClass
public class ListsX {

  public <T, U> U collect(Collection<T> list, Collector<? super T, ?, U> collector) {
    if (list == null) {
      return null;
    }
    return list.stream().collect(collector);
  }

  public <T, U> Function<Collection<T>, U> collecting(Collector<? super T, ?, U> collector) {
    return list -> collect(list, collector);
  }

  @SafeVarargs
  public <T> List<T> cons(T head, List<T>... tails) {
    return Stream.concat(
      Stream.of(head),
      Arrays.stream(tails).flatMap(Collection::stream)
    ).toList();
  }

  public <T> Option<T> find(Collection<T> list, Predicate<? super T> predicate) {
    if (list == null) {
      return null;
    }
    return AnStream.from(list).findFirstAsOption(predicate);
  }

  public <T, U> List<U> flatMap(List<T> list, Function<? super T, List<U>> mapper) {
    if (list == null) {
      return null;
    }
    return list.stream().map(mapper).flatMap(Collection::stream).toList();
  }

  public <T> List<T> insertionSort(List<T> list, Comparator<? super T> comparator) {
    //noinspection unchecked
    T[] result = (T[]) new Object[list.size()];
    for (int i = 1; i < result.length; i++) {
      T x = result[i];
      int j = i;
      for (; j > 0 && comparator.compare(result[j - 1], x) > 0; j--) {
        result[j] = result[j - 1];
      }
      result[j] = x;
    }
    return Arrays.asList(result);
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
    return AnStream.from(collection).allow(clazz).toList();
  }

}
