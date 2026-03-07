package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.spoon.path.SpoonPaths;
import com.mategka.dava.analyzer.struct.property.index.PropertyKey;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@PropertyKey("path")
public record PathProperty(String value) implements StringProperty {

  @Contract("_ -> new")
  public static @NotNull PathProperty fromSpoonPathProperty(@NotNull SpoonPathProperty spoonPathProperty) {
    return new PathProperty(SpoonPaths.simplify(spoonPathProperty.value()));
  }

  @Override
  public String toString() {
    return value;
  }

}
