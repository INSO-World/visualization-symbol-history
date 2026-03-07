package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;

import spoon.reflect.declaration.CtNamedElement;

@PropertyKey("simpleName")
public record SimpleNameProperty(String value) implements StringProperty {

  public static final String ROOT_PACKAGE_NAME = "ROOT";

  public static SimpleNameProperty forRootPackage() {
    return new SimpleNameProperty(ROOT_PACKAGE_NAME);
  }

  public static SimpleNameProperty fromElement(CtNamedElement element) {
    return new SimpleNameProperty(element.getSimpleName());
  }

  @Override
  public String toString() {
    return value;
  }

}
