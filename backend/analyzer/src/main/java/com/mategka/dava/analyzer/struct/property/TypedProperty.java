package com.mategka.dava.analyzer.struct.property;

public sealed interface TypedProperty<T> extends Property
  permits ListProperty, MapProperty, NullableProperty, OptionalProperty, SetProperty, SimpleProperty {

  T value();

}
