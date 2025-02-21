package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.extension.CollectorsX;
import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.property.value.type.Type;

import java.util.List;

@PropertyKey("supertypes")
public record SupertypesProperty(List<Type> value) implements ListProperty<Type> {

  @Override
  public String toString() {
    return "extends " + value.stream().collect(CollectorsX.commaSeparated());
  }

}
