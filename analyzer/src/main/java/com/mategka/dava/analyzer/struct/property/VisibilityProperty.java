package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.property.value.Visibility;

@PropertyKey("visibility")
public record VisibilityProperty(Visibility value) implements EnumProperty<Visibility> {

  @Override
  public String toString() {
    return value.getPseudoKeyword();
  }

}
