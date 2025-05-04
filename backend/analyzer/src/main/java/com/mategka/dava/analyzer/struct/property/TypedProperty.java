package com.mategka.dava.analyzer.struct.property;

public sealed interface TypedProperty<T> extends Property
  permits CollectionProperty, CtPathProperty, SerializableProperty {

  T value();

}
