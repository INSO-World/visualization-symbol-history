package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.symbol.Symbol;

import java.util.Optional;

@PropertyKey("parent")
public record ParentProperty(Optional<TypeValue.KnownType> value) implements OptionalProperty<TypeValue.KnownType> {

  public ParentProperty(Long id) {
    this(Optional.ofNullable(id).map(TypeValue.KnownType::of));
  }

  public ParentProperty(Symbol symbol) {
    this(Optional.ofNullable(symbol).map(Symbol::getId).map(TypeValue.KnownType::of));
  }

  @Override
  public String toString() {
    return value.map(TypeValue.KnownType::toString).orElse("(none)");
  }

}
