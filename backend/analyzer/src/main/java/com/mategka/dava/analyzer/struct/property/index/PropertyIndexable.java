package com.mategka.dava.analyzer.struct.property.index;

import com.mategka.dava.analyzer.struct.property.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface PropertyIndexable {

  @NotNull
  Map<String, Property> getProperties();

  default Property getProperty(String propertyKey) {
    return getProperties().get(propertyKey);
  }

  default <T extends Property> T getProperty(@NotNull Class<T> propertyClass) {
    return propertyClass.cast(getProperties().get(PropertyKeys.get(propertyClass)));
  }

}
