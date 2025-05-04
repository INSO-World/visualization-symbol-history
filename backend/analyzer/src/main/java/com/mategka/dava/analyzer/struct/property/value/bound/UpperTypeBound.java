package com.mategka.dava.analyzer.struct.property.value.bound;

import com.mategka.dava.analyzer.struct.property.value.type.Type;
import com.mategka.dava.analyzer.struct.property.value.type.UnknownType;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Map;

public record UpperTypeBound(@NotNull Type argument) implements TypeBound {

  @Serial
  private static final long serialVersionUID = -4447973208436414294L;

  @Override
  public Map<String, Type> getJsonValue() {
    return Map.of("extends", argument);
  }

  @Override
  public boolean isEmpty() {
    return argument instanceof UnknownType unknownType && unknownType.getQualifiedName().equals("java.lang.Object");
  }

  @Override
  public String toString() {
    return "extends " + argument;
  }

}
