package com.mategka.dava.analyzer.struct.property.value.argument;

import com.mategka.dava.analyzer.struct.property.value.bound.TypeBound;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Map;

public record WildcardTypeArgument(@NotNull TypeBound bound) implements TypeArgument {

  @Serial
  private static final long serialVersionUID = -730840105290060364L;

  @Override
  public String toString() {
    return "? %s".formatted(bound);
  }

  @Override
  public Map<String, Object> toJsonValue() {
    if (bound.isEmpty()) {
      return Map.of("wildcard", "any");
    }
    return Map.of("wildcard", bound);
  }

}
