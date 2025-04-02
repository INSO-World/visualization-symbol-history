package com.mategka.dava.analyzer.struct.property.index;

import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.option.Options;
import com.mategka.dava.analyzer.struct.property.NullableProperty;
import com.mategka.dava.analyzer.struct.property.Property;
import com.mategka.dava.analyzer.struct.property.TypedProperty;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Map;

public interface PropertyIndexable {

  @NotNull
  Map<String, Property> getProperties();

  @UnknownNullability
  default Property getProperty(String propertyKey) {
    return getProperties().get(propertyKey);
  }

  default <T extends Property> T getProperty(@NotNull Class<T> propertyClass) {
    return propertyClass.cast(getProperties().get(PropertyKeys.get(propertyClass)));
  }

  default <T> Option<T> getPropertyValue(@NotNull Class<? extends TypedProperty<T>> propertyClass) {
    var property = getProperty(propertyClass);
    return switch (property) {
      case null -> Option.None();
      case NullableProperty<T> nullableProperty -> nullableProperty.asOption();
      default -> Options.fromNullable(property.value());
    };
  }

}
