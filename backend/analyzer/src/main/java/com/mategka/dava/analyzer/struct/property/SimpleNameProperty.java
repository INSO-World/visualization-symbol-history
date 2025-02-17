package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.symbol.Symbol;

import spoon.reflect.declaration.CtNamedElement;

@PropertyKey("simpleName")
public record SimpleNameProperty(String value) implements SimpleProperty<String> {

  public static SimpleNameProperty fromElement(CtNamedElement element) {
    return new SimpleNameProperty(element.getSimpleName());
  }

  public static SimpleNameProperty forRootPackage() {
    return new SimpleNameProperty(Symbol.ROOT_PACKAGE_NAME);
  }

  @Override
  public String toString() {
    return value;
  }

}
