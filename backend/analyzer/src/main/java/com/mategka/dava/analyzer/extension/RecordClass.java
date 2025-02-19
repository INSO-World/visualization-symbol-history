package com.mategka.dava.analyzer.extension;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RecordClass<R extends Record> {

  Class<R> clazz;
  List<Method> accessors;

  private RecordClass(@NotNull Class<R> clazz) {
    this.clazz = clazz;
    accessors = Arrays.stream(clazz.getRecordComponents())
      .map(RecordComponent::getAccessor)
      .toList();
  }

  @Contract("_ -> new")
  public static <R extends Record> @NotNull RecordClass<R> fromClass(@NotNull Class<R> clazz) {
    return new RecordClass<>(clazz);
  }

  public String getSimpleName() {
    return clazz.getSimpleName();
  }

  public boolean isInstance(Object obj) {
    return clazz.isInstance(obj);
  }

  public @NotNull List<Object> destructure(@NotNull R instance) {
    return accessors.stream()
      .map(m -> {
        try {
          return m.invoke(instance);
        } catch (IllegalAccessException | InvocationTargetException e) {
          return new IllegalStateException(
            "Field values could not be retrieved for %s instance".formatted(clazz.getSimpleName())
          );
        }
      })
      .toList();
  }

}
