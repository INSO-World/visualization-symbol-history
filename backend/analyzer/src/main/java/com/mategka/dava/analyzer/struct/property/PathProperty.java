package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.spoon.Spoon;
import com.mategka.dava.analyzer.struct.property.index.PropertyKey;

import org.jetbrains.annotations.NotNull;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.path.CtPath;

@PropertyKey("path")
public record PathProperty(CtPath value) implements SimpleProperty<CtPath> {

  public static PathProperty fromElement(@NotNull CtElement element) {
    return new PathProperty(Spoon.pathOf(element));
  }

  @Override
  public String toString() {
    return value.toString();
  }

}
