package com.mategka.dava.analyzer.extension.struct;

import com.mategka.dava.analyzer.extension.ListsX;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RecordClass<R extends Record> {

  Class<R> clazz;
  List<RecordComponent> components;

  private RecordClass(@NotNull Class<R> clazz) {
    this.clazz = clazz;
    components = Arrays.asList(clazz.getRecordComponents());
  }

  @Contract("_ -> new")
  public static <R extends Record> @NotNull RecordClass<R> fromClass(@NotNull Class<R> clazz) {
    return new RecordClass<>(clazz);
  }

  public @NotNull List<Object> destructure(@NotNull R instance) {
    return ListsX.map(
      components, component -> {
        try {
          return component.getAccessor().invoke(instance);
        } catch (ReflectiveOperationException e) {
          throw new IllegalStateException(
            "Field values could not be retrieved for %s instance".formatted(getSimpleName())
          );
        }
      }
    );
  }

  public String getSimpleName() {
    return clazz.getSimpleName();
  }

  public boolean isInstance(Object obj) {
    return clazz.isInstance(obj);
  }

}
