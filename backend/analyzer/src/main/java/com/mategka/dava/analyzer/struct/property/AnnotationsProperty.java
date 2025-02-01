package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;

import java.util.List;
import java.util.stream.Collectors;

@PropertyKey("annotations")
public record AnnotationsProperty(List<? extends TypeValue> value) implements ListProperty<TypeValue> {

  @Override
  public String toString() {
    return value.stream()
      .map(v -> "@" + v.toString())
      .collect(Collectors.joining(" "));
  }

}
