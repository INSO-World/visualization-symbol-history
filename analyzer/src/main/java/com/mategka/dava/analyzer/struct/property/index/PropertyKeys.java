package com.mategka.dava.analyzer.struct.property.index;

import com.mategka.dava.analyzer.struct.property.Property;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class PropertyKeys {

  public @NotNull String get(Class<? extends Property> propertyClass) {
    var keyAnnotation = propertyClass.getAnnotation(PropertyKey.class);
    if (keyAnnotation != null) {
      return keyAnnotation.value();
    }
    throw new IllegalArgumentException(propertyClass.getSimpleName() + " does not have a @PropertyKey annotation");
  }

}
