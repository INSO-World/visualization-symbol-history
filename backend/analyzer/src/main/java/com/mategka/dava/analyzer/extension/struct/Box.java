package com.mategka.dava.analyzer.extension.struct;

import com.mategka.dava.analyzer.extension.stream.AnStream;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

public class Box<T> {

  private T value;

  public Box(@NonNull T value) {
    this.value = value;
  }

  public static <T> Collection<Box<T>> collect(@NotNull Collection<T> collection) {
    return collection.stream().map(Box::new).collect(Collectors.toList());
  }

  public static <T> AnStream<T> stream(@NotNull Collection<T> collection) {
    return AnStream.from(collection).map(Box::new).map(Box::consume);
  }

  public T consume() {
    var result = value;
    value = null;
    return result;
  }

  public T peek() {
    return value;
  }

}
