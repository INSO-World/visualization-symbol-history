package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;

import org.jetbrains.annotations.NotNull;

@PropertyKey("path")
public record PathProperty(String value) implements SimpleProperty<String> {

  public static PathProperty fromCtPathProperty(@NotNull CtPathProperty ctPathProperty) {
    return new PathProperty(ctPathProperty.value().toUnorderedString());
  }

  @Override
  public String toString() {
    return value;
  }

}
