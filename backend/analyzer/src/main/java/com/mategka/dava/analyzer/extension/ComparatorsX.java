package com.mategka.dava.analyzer.extension;

import com.leakyabstractions.result.core.Results;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.function.Predicate;

@UtilityClass
public class ComparatorsX {

  public int compare(@NotNull Object left, @NotNull Object right) {
    if (left instanceof Comparable<?>) {
      //noinspection unchecked
      return Results.ofCallable(() -> ((Comparable<Object>) left).compareTo(right)).orElse(0);
    }
    return 0;
  }

  public <T> Comparator<T> falseFirst(Predicate<T> keyMapper) {
    return Comparator.comparingInt(t -> keyMapper.test(t) ? 0 : -1);
  }

  public Comparator<Object> nullsFirstComparator() {
    return Comparator.nullsFirst(ComparatorsX::compare);
  }

  public <T> Comparator<T> trueFirst(Predicate<T> keyMapper) {
    return Comparator.comparingInt(t -> keyMapper.test(t) ? -1 : 0);
  }

}
