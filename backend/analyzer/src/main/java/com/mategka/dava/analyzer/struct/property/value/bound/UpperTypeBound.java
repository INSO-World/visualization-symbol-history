package com.mategka.dava.analyzer.struct.property.value.bound;

import com.mategka.dava.analyzer.struct.property.value.type.Type;

import org.jetbrains.annotations.NotNull;

public record UpperTypeBound(@NotNull Type argument) implements TypeBound {

  @Override
  public String toString() {
    return "extends " + argument;
  }

}
