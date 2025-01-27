package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.symbol.Symbol;

import org.jetbrains.annotations.NotNull;

@PropertyKey("parent")
public record ParentProperty(TypeValue.KnownType value) implements NullableProperty<TypeValue.KnownType> {

  public static ParentProperty fromId(long id) {
    return new ParentProperty(TypeValue.KnownType.of(id));
  }

  public static ParentProperty fromSymbol(@NotNull Symbol symbol) {
    return fromId(symbol.getId());
  }

  @Override
  public String toString() {
    return asOptional().map(TypeValue.KnownType::toString).orElse("(none)");
  }

}
