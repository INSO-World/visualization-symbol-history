package com.mategka.dava.analyzer.extension;

import lombok.experimental.UtilityClass;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

@UtilityClass
public class RecordsX {

  public <R> Function<R, Stream<Object>> componentExtractor(Class<R> recordClass) {
    var accessors = Arrays.stream(recordClass.getRecordComponents())
      .map(RecordComponent::getAccessor)
      .toList();
    return (instance) -> accessors.stream().map(m -> {
      try {
        return m.invoke(instance);
      } catch (IllegalAccessException | InvocationTargetException e) {
        return new IllegalStateException(
          "Field values could not be retrieved for %s instance".formatted(recordClass.getSimpleName())
        );
      }
    });
  }

}
