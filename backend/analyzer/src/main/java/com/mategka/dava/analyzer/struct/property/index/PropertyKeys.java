package com.mategka.dava.analyzer.struct.property.index;

import com.mategka.dava.analyzer.struct.property.Property;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class PropertyKeys {

  public @NotNull String get(Class<? extends Property> propertyClass) {
    return propertyClass.getAnnotation(PropertyKey.class).value();
  }

  public boolean isUndefined(final String key) {
    return PropertyKey.UNDEFINED.equals(key);
  }

}
