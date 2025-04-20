package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.spoon.CtEqPath;
import com.mategka.dava.analyzer.struct.property.index.PropertyKey;

import org.jetbrains.annotations.NotNull;
import spoon.reflect.declaration.CtElement;

@PropertyKey("ctpath")
public record CtPathProperty(CtEqPath value) implements SimpleProperty<CtEqPath> {

  public static final CtPathProperty EMPTY = new CtPathProperty(CtEqPath.EMPTY);

  public static CtPathProperty fromElement(@NotNull CtElement element) {
    return new CtPathProperty(CtEqPath.of(element));
  }

  @Override
  public String toString() {
    return value.toString();
  }

}
