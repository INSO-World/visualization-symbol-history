package com.mategka.dava.analyzer.extension;

import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.struct.Pair;

import com.google.common.collect.Iterables;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

import static com.mategka.dava.analyzer.extension.option.Option.None;
import static com.mategka.dava.analyzer.extension.option.Option.Some;

@UtilityClass
public class IterablesX {

  public <T> Iterable<T> consuming(@NotNull @Mutable Iterable<T> iterable) {
    return Iterables.consumingIterable(iterable);
  }

  public <T> T getFirst(@NotNull Iterable<T> iterable) {
    var iterator = iterable.iterator();
    if (!iterator.hasNext()) {
      throw new IllegalArgumentException("Iterable was empty");
    }
    return iterator.next();
  }

  public <T> T getOnlyElement(@NotNull Iterable<T> iterable) {
    var iterator = iterable.iterator();
    if (!iterator.hasNext()) {
      throw new IllegalArgumentException("Iterable was empty");
    }
    var element = iterator.next();
    if (iterator.hasNext()) {
      throw new IllegalArgumentException("Iterable had more than one element");
    }
    return element;
  }

  public <T extends Comparable<? super T>> Option<Pair<T, T>> minmax(@NotNull Iterator<T> iterator) {
    if (!iterator.hasNext()) {
      return None();
    }
    T min = iterator.next();
    T max = min;
    while (iterator.hasNext()) {
      T next = iterator.next();
      if (next.compareTo(min) < 0) {
        min = next;
      } else if (next.compareTo(max) > 0) {
        max = next;
      }
    }
    return Some(Pair.of(min, max));
  }

}
