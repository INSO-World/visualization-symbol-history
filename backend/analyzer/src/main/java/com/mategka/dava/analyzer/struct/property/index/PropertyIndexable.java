package com.mategka.dava.analyzer.struct.property.index;

import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.option.Options;
import com.mategka.dava.analyzer.struct.property.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.io.Serializable;
import java.util.Map;

public interface PropertyIndexable {

  @NotNull
  Map<String, Property> getProperties();

  @UnknownNullability
  default Property getProperty(String propertyKey) {
    return getProperties().get(propertyKey);
  }

  default boolean containsProperty(@NotNull Class<? extends Property> propertyClass) {
    return getProperties().containsKey(PropertyKeys.get(propertyClass));
  }

  default <T extends Property> Option<T> getProperty(Class<T> propertyClass) {
    return Options.fromNullable(getProperties().get(PropertyKeys.get(propertyClass))).narrow(propertyClass);
  }

  @SuppressWarnings("unchecked")
  default <T> Option<T> getPropertyValue(@NotNull Class<? extends TypedProperty<T>> propertyClass) {
    return getProperty(propertyClass).flatMap(property -> switch (property) {
      // NOTE: Lower bound change necessitates type cast despite being logically sound as per subtype relationship
      case SerializableProperty<?> serializableProperty -> (Option<T>) getSerializablePropertyValue(serializableProperty);
      case null -> Option.None();
      default -> Options.fromNullable(property.value());
    });
  }

  static <T extends Serializable> Option<T> getSerializablePropertyValue(@NotNull SerializableProperty<T> property) {
    return switch (property) {
      case NullableProperty<T> nullableProperty -> nullableProperty.asOption();
      case SimpleProperty<T> simpleProperty -> Option.Some(simpleProperty.value());
      default -> Options.fromNullable(property.value());
    };
  }

}
