package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@PropertyKey("body")
public record BodyHashProperty(@NotNull Integer value) implements SimpleProperty<@NotNull Integer> {

  @Contract("_ -> new")
  public static @NotNull BodyHashProperty fromString(String string) {
    return new BodyHashProperty(("/~" + string + "~/").hashCode());
  }

  @Override
  public String toString() {
    return "%08X".formatted(value);
  }

}
