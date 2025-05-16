package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.symbol.Symbol;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@PropertyKey("parent")
public record ParentProperty(@NotNull Long value) implements NullableProperty<@NotNull Long> {

  @Contract("_ -> new")
  public static @NotNull ParentProperty fromSymbol(@NotNull Symbol symbol) {
    return new ParentProperty(symbol.getKey().symbolId());
  }

  @Override
  public String toString() {
    return asOption().map(String::valueOf).getOrElse("(none)");
  }

}
