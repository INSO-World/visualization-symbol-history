package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.struct.property.index.PropertyIndexable;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;

import lombok.NonNull;
import lombok.Value;

import java.util.Set;

@Value
public class SymbolUpdate2 implements PropertyIndexable {

  @NonNull
  SymbolKey sourceKey;

  @NonNull
  Symbol2.Context targetContext;

  @NonNull
  PropertyMap properties;

  @NonNull
  Set<UpdateFlag> flags;

}
