package com.mategka.dava.analyzer.extension;

import com.leakyabstractions.result.core.Results;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

@UtilityClass
public class ObjectsX {

  public Comparator<Object> nullsFirstComparator() {
    return Comparator.nullsFirst(ObjectsX::compare);
  }

  public int compare(@NotNull Object left, @NotNull Object right) {
    if (left instanceof Comparable<?>) {
      //noinspection unchecked
      return Results.ofCallable(() -> ((Comparable<Object>) left).compareTo(right)).orElse(0);
    }
    return 0;
  }

}
