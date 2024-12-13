package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.property.index.PropertyKeys;
import org.jetbrains.annotations.NotNull;

@PropertyKey(PropertyKey.UNDEFINED)
public sealed interface Property permits ListProperty, MapProperty, OptionalProperty, SetProperty, SimpleProperty {

  static Object getDefault(Class<? extends Property> propertyClass) {
    return Properties.getDefault(propertyClass);
  }

  default @NotNull String getKey() {
    return PropertyKeys.get(getClass());
  }

  default <T extends Property> T as(Class<T> propertyClass) {
    if (propertyClass.isInstance(this)) {
      return propertyClass.cast(this);
    }
    throw new IllegalArgumentException(
      "Property %s is no %s".formatted(getClass().getSimpleName(), propertyClass.getSimpleName())
    );
  }

  Object value();

}
