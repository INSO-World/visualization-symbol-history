package com.mategka.dava.analyzer.struct.property.value.argument;

import com.mategka.dava.analyzer.struct.property.value.type.Type;

import org.jetbrains.annotations.NotNull;

public record ConcreteTypeArgument(@NotNull Type type) implements TypeArgument {

  @Override
  public String toString() {
    return type.toString();
  }

}
