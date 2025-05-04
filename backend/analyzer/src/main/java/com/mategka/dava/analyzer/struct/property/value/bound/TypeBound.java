package com.mategka.dava.analyzer.struct.property.value.bound;

import com.mategka.dava.analyzer.struct.property.value.type.Type;

import com.fasterxml.jackson.annotation.JsonValue;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Map;

public sealed interface TypeBound extends Serializable permits UpperTypeBound, LowerTypeBound {

  static LowerTypeBound lower(@NotNull Type type) {
    return new LowerTypeBound(type);
  }

  static UpperTypeBound upper(@NotNull Type type) {
    return new UpperTypeBound(type);
  }

  @NotNull Type argument();

  boolean isEmpty();

  @JsonValue
  Map<String, Type> getJsonValue();

}
