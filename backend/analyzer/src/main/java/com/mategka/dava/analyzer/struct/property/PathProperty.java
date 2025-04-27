package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.spoon.SpoonPathElement;
import com.mategka.dava.analyzer.struct.property.index.PropertyKey;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@PropertyKey("path")
public record PathProperty(String value) implements SimpleProperty<String> {

  @Deprecated
  public static PathProperty fromCtPathProperty(@NotNull CtPathProperty ctPathProperty) {
    return new PathProperty(ctPathProperty.value().toUnorderedString());
  }

  @Contract("_ -> new")
  public static @NotNull PathProperty fromSpoonPathProperty(@NotNull SpoonPathProperty spoonPathProperty) {
    return new PathProperty(SpoonPathElement.simplify(spoonPathProperty.value()));
  }

  @Override
  public String toString() {
    return value;
  }

}
