package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.property.value.type.KnownType;
import com.mategka.dava.analyzer.struct.symbol.Symbol;
import com.mategka.dava.analyzer.struct.symbol.Symbol2;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@PropertyKey("parent")
public record ParentProperty(KnownType value) implements NullableProperty<KnownType> {

  public static ParentProperty fromId(long id) {
    return new ParentProperty(KnownType.of(id));
  }

  public static ParentProperty fromSymbol(@NotNull Symbol symbol) {
    return fromId(symbol.getKey().symbolId());
  }

  @Contract("_ -> new")
  public static @NotNull ParentProperty fromSymbol(@NotNull Symbol2 symbol) {
    return fromId(symbol.getKey().symbolId());
  }

  @Override
  public String toString() {
    return asOption().map(KnownType::toString).getOrElse("(none)");
  }

}
