package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.property.value.TypeParameter;

import java.util.List;
import java.util.stream.Collectors;

@PropertyKey("typeParameters")
public record TypeParametersProperty(List<TypeParameter> value) implements ListProperty<TypeParameter> {

  @Override
  public String toString() {
    return "<%s>".formatted(value.stream().map(TypeParameter::toString).collect(Collectors.joining(", ")));
  }

}
