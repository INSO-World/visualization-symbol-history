package com.mategka.dava.analyzer.extension;

import com.google.common.collect.Iterables;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

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

}
