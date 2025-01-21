package com.mategka.dava.analyzer.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

@UtilityClass
public class Streams {

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

  public <K, V1, V2> Function<Map.Entry<K, V1>, Map.Entry<K, V2>> mappingValue(Function<? super V1, ? extends V2> mapper) {
    return entry -> Map.entry(entry.getKey(), mapper.apply(entry.getValue()));
  }

}
