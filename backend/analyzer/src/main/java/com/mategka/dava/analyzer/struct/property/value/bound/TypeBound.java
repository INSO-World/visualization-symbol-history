package com.mategka.dava.analyzer.struct.property.value.bound;

import com.mategka.dava.analyzer.struct.property.value.type.Type;

import org.jetbrains.annotations.NotNull;

public sealed interface TypeBound permits UpperTypeBound, LowerTypeBound {

  static LowerTypeBound lower(@NotNull Type type) {
    return new LowerTypeBound(type);
  }

  static UpperTypeBound upper(@NotNull Type type) {
    return new UpperTypeBound(type);
  }

  @NotNull Type argument();

}
