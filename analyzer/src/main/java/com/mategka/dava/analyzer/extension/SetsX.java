package com.mategka.dava.analyzer.extension;

import com.mategka.dava.analyzer.extension.stream.AnStream;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
public class SetsX {

  public <E extends Enum<E>> Set<E> containedEnumValues(Class<E> enumClass, @NotNull Iterable<E> iterable) {
    EnumSet<E> values = EnumSet.noneOf(enumClass);
    int maxSize = EnumSet.allOf(enumClass).size();
    for (E e : iterable) {
      if (e == null) {
        continue;
      }
      values.add(e);
      if (values.size() == maxSize) {
        break;
      }
    }
    return values;
  }

  public <E> Set<E> copy(Set<E> set, Function<E, E> copyFn) {
    return AnStream.from(set).map(copyFn).toSet();
  }

  public <E extends Copyable<E>> Set<E> copy(Set<E> set) {
    return AnStream.from(set).map(Copyable::copy).toSet();
  }

  public <E> Set<E> difference(Set<E> minuend, Set<?> @NotNull ... subtrahends) {
    if (subtrahends.length == 0) {
      return minuend;
    }
    var subtrahend = union(subtrahends);
    return minuend.stream()
      .filter(e -> !subtrahend.contains(e))
      .collect(Collectors.toSet());
  }

  @SafeVarargs
  public <E> Set<E> intersection(Set<? extends E> @NotNull ... sets) {
    if (sets.length == 0) {
      return Collections.emptySet();
    }
    Set<E> result = new HashSet<>(sets[0]);
    for (int i = 1; i < sets.length; i++) {
      result.retainAll(sets[i]);
    }
    return result;
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
