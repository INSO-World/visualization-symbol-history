package com.mategka.dava.analyzer.struct.property.index;

import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.option.Options;
import com.mategka.dava.analyzer.struct.property.Property;
import com.mategka.dava.analyzer.struct.property.SimpleProperty;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class PropertyMap extends HashMap<String, Property> {

  public PropertyMap() {
    super();
  }

  public PropertyMap(@NotNull final Map<String, Property> properties) {
    super(properties);
  }

  public static PropertyMap of(Property... properties) {
    var result = new PropertyMap();
    for (Property property : properties) {
      result.put(property);
    }
    return result;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static <T extends Map.Entry<String, Property>> Collector<T, ?, PropertyMap> collectEntries() {
    return collector(Map.Entry::getKey, Map.Entry::getValue);
  }

  public static <T extends Property> Collector<T, ?, PropertyMap> collectProperties() {
    return collector(Property::getKey, Function.identity());
  }

  public static <T> Collector<T, ?, PropertyMap> collector(
    @NotNull Function<? super T, String> keyFunction,
    Function<? super T, ? extends Property> valueFunction
  ) {
    return Collectors.toMap(keyFunction, valueFunction, (older, newer) -> newer, PropertyMap::new);
  }

  public PropertyMap diff(@NotNull Collection<Property> newProperties) {
    return newProperties.stream()
      .filter(p -> Option.Some(p.getKey())
        .map(this::get)
        .map(Property::value)
        .map(v -> !v.equals(p.value()))
        .getOrElse(false)
      )
      .collect(PropertyMap.collectProperties());
  }

  public PropertyMap diff(@NotNull PropertyIndexable newProperties) {
    return diff(newProperties.getProperties().values());
  }

  public <T extends Property> Option<T> get(Class<T> propertyClass) {
    return Options.fromNullable(super.get(PropertyKeys.get(propertyClass))).narrow(propertyClass);
  }

  public <T> T getOrDefault(Class<? extends SimpleProperty<T>> propertyClass, T defaultValue) {
    return get(propertyClass).map(SimpleProperty::value).getOrElse(defaultValue);
  }

  public Property put(Property property) {
    return super.put(property.getKey(), property);
  }

  public void putAll(Collection<? extends Property> c) {
    super.putAll(c.stream().collect(collectProperties()));
  }

  public Property putIfAbsent(Property property) {
    return super.putIfAbsent(property.getKey(), property);
  }

  public Builder toBuilder() {
    return new Builder().properties(this);
  }

  public PropertyMap with(Property property) {
    return toBuilder().property(property).build();
  }

  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @Override
  public PropertyMap clone() {
    return toBuilder().build();
  }

  public static class Builder {

    private final PropertyMap propertyMap = new PropertyMap();

    private Builder() {
    }

    public PropertyMap build() {
      return propertyMap;
    }

    public Builder properties(@NonNull PropertyMap propertyMap) {
      this.propertyMap.putAll(propertyMap);
      return this;
    }

    public Builder property(@NonNull Option<? extends Property> property) {
      return property.fold(this::property, () -> this);
    }

    public Builder property(@NonNull Property property) {
      propertyMap.put(property);
      return this;
    }

    public String toString() {
      return "PropertyMap.PropertyMapBuilder(propertyMap=" + this.propertyMap + ")";
    }

  }

}
