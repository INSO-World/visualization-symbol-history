package com.mategka.dava.analyzer.struct.property;

public sealed interface TypedProperty<T> extends Property
  permits ListProperty, MapProperty, NullableProperty, OptionalProperty, SetProperty, SimpleProperty {

  @SuppressWarnings("unchecked")
  static <T> T getDefault(Class<? extends TypedProperty<T>> propertyClass) {
    return (T) Properties.getDefault(propertyClass);
  }

  T value();

}
