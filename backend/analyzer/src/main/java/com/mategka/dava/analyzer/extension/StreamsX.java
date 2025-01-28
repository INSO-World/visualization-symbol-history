package com.mategka.dava.analyzer.extension;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

@UtilityClass
public class StreamsX {

  public Stream<String> splitting(String string, @NotNull String delimiter) {
    if (string == null) {
      return Stream.empty();
    }
    return Stream.of(string.split(delimiter));
  }

  public Stream<String> splitting(String string, @NotNull String delimiter, int limit) {
    if (string == null) {
      return Stream.empty();
    }
    return Stream.of(string.split(delimiter, limit));
  }

  public <K, V1, V2> Function<Map.Entry<K, V1>, Map.Entry<K, V2>> mappingValue(
    Function<? super V1, ? extends V2> mapper) {
    return entry -> Map.entry(entry.getKey(), mapper.apply(entry.getValue()));
  }

  public <T> Comparator<T> falseFirst(Predicate<T> keyMapper) {
    return Comparator.comparingInt(t -> keyMapper.test(t) ? 0 : -1);
  }

  public <T> Comparator<T> trueFirst(Predicate<T> keyMapper) {
    return Comparator.comparingInt(t -> keyMapper.test(t) ? -1 : 0);
  }

  @SafeVarargs
  public <T> Stream<T> cons(T head, Stream<? extends T>... tails) {
    if (tails.length == 0) {
      return Stream.of(head);
    }
    return Stream.concat(
      Stream.of(head),
      Arrays.stream(tails).flatMap(Function.identity())
    );
  }

}
