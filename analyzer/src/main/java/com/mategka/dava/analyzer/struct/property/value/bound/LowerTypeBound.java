package com.mategka.dava.analyzer.struct.property.value.bound;

import com.mategka.dava.analyzer.struct.property.value.type.Type;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Map;

public record LowerTypeBound(@NotNull Type argument) implements TypeBound {

  @Serial
  private static final long serialVersionUID = 6788905794023822491L;

  @Override
  public Map<String, Type> getJsonValue() {
    return Map.of("super", argument);
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public String toString() {
    return "super " + argument;
  }

}
