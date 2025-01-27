package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.symbol.Symbol;

import org.jetbrains.annotations.NotNull;

@PropertyKey("parent")
public record ParentProperty(TypeValue.KnownType value) implements NullableProperty<TypeValue.KnownType> {

  public ParentProperty(long id) {
    this(TypeValue.KnownType.of(id));
  }

  public ParentProperty(@NotNull Symbol symbol) {
    this(symbol.getId());
  }

  @Override
  public String toString() {
    return asOptional().map(TypeValue.KnownType::toString).orElse("(none)");
  }

}
