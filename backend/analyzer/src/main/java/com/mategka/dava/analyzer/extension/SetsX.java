package com.mategka.dava.analyzer.extension;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
public class SetsX {

  public <E> Set<E> difference(Set<E> minuend, Set<?> @NotNull ... subtrahends) {
    if (subtrahends.length == 0) {
      return minuend;
    }
    var subtrahend = union(subtrahends);
    return minuend.stream()
      .filter(e -> !subtrahend.contains(e))
      .collect(Collectors.toSet());
  }

  public <E, T> Set<T> keysOf(Collection<E> collection, Function<E, T> keyMapper) {
    return collection.stream().map(keyMapper).collect(Collectors.toSet());
  }

  @SafeVarargs
  public <E> Set<E> union(Set<? extends E> @NotNull ... sets) {
    if (sets.length == 0) {
      return Collections.emptySet();
    }
    if (sets.length == 1) {
      return Covariant.set(sets[0]);
    }
    return Arrays.stream(sets)
      .flatMap(Collection::stream)
      .collect(Collectors.toSet());
  }

}
