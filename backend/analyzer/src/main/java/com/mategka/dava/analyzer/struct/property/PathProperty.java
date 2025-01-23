package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import spoon.reflect.path.CtPath;

@PropertyKey("path")
public record PathProperty(CtPath value) implements SimpleProperty<CtPath> {

  @Override
  public String toString() {
    return value.toString();
  }

}
