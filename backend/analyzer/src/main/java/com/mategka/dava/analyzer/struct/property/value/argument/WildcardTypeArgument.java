package com.mategka.dava.analyzer.struct.property.value.argument;

import com.mategka.dava.analyzer.struct.property.value.bound.TypeBound;

import org.jetbrains.annotations.NotNull;

public record WildcardTypeArgument(@NotNull TypeBound bound) implements TypeArgument {

  @Override
  public String toString() {
    return "? %s".formatted(bound);
  }

}
