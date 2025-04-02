package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.spoon.CtEqPath;
import com.mategka.dava.analyzer.struct.property.index.PropertyKey;

import org.jetbrains.annotations.NotNull;
import spoon.reflect.declaration.CtElement;

@PropertyKey("path")
public record PathProperty(CtEqPath value) implements SimpleProperty<CtEqPath> {

  public static final PathProperty EMPTY = new PathProperty(CtEqPath.EMPTY);

  public static PathProperty fromElement(@NotNull CtElement element) {
    return new PathProperty(CtEqPath.of(element));
  }

  @Override
  public String toString() {
    return value.toString();
  }

}
