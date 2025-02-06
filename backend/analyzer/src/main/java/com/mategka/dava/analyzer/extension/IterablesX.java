package com.mategka.dava.analyzer.extension;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class IterablesX {

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
