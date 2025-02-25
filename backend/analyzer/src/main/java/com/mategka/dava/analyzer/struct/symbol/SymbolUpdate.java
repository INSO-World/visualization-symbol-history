package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.struct.property.index.PropertyIndexable;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;

import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@Value
public class SymbolUpdate implements PropertyIndexable {

  @NonNull
  SymbolKey key;

  @NonNull
  String commitSha;

  @NonNull
  PropertyMap properties;

  @NonNull
  Set<UpdateFlag> flags;

  public boolean appliesTo(@NotNull Symbol symbol) {
    return key.equals(symbol.getKey());
  }

}
