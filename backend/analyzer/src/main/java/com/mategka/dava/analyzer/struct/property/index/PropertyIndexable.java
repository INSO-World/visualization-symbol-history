package com.mategka.dava.analyzer.struct.property.index;

import com.mategka.dava.analyzer.struct.property.*;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public interface PropertyIndexable {

  @NotNull
  Map<String, Property> getProperties();

  default Property getProperty(String propertyKey) {
    return getProperties().get(propertyKey);
  }

  default <T extends Property> T getProperty(@NotNull Class<T> propertyClass) {
    return propertyClass.cast(getProperties().get(PropertyKeys.get(propertyClass)));
  }

  @SuppressWarnings("unchecked")
  default <T> Optional<T> getPropertyValue(@NotNull Class<? extends TypedProperty<T>> propertyClass) {
    var property = getProperty(propertyClass);
    return switch (property) {
      case null -> Optional.empty();
      case OptionalProperty<?> optionalProperty -> (Optional<T>) optionalProperty.value();
      case NullableProperty<T> nullableProperty -> nullableProperty.asOptional();
      default -> Optional.of(property.value());
    };
  }

}
