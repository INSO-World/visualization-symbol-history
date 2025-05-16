package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.property.value.type.Type;

import java.util.List;
import java.util.stream.Collectors;

@PropertyKey("annotations")
public record AnnotationsProperty(List<Type> value) implements ListProperty<Type> {

  @Override
  public String toString() {
    return value.stream()
      .map(v -> "@" + v.toString())
      .collect(Collectors.joining(" "));
  }

}
