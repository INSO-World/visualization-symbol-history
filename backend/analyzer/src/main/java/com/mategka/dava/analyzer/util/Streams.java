package com.mategka.dava.analyzer.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

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

}
