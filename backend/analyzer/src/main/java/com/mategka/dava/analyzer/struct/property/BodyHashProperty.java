package com.mategka.dava.analyzer.struct.property;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record BodyHashProperty(@NotNull Integer value) implements SimpleProperty<@NotNull Integer> {

  @Contract("_ -> new")
  public static @NotNull BodyHashProperty fromString(String string) {
    return new BodyHashProperty(("/~" + string + "~/").hashCode());
  }

}
