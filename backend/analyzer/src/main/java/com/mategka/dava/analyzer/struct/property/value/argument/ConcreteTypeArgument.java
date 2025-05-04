package com.mategka.dava.analyzer.struct.property.value.argument;

import com.mategka.dava.analyzer.struct.property.value.type.Type;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Map;

public record ConcreteTypeArgument(@NotNull Type type) implements TypeArgument {

  @Serial
  private static final long serialVersionUID = 2130450362455471538L;

  @Override
  public Map<String, Object> toJsonValue() {
    return Map.of("type", type);
  }

  @Override
  public String toString() {
    return type.toString();
  }

}
