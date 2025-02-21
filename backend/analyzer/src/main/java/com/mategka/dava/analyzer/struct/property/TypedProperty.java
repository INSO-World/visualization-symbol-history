package com.mategka.dava.analyzer.struct.property;

public sealed interface TypedProperty<T> extends Property
  permits ListProperty, NullableProperty, SetProperty, SimpleProperty {

  T value();

}
