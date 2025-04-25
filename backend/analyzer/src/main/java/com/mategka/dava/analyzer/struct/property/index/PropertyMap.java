package com.mategka.dava.analyzer.struct.property.index;

import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.option.Options;
import com.mategka.dava.analyzer.struct.property.*;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PropertyMap extends HashMap<String, Property> {

  private static final Set<String> ABBREVIATED_PROPERTIES = Stream.of(
      CtPathProperty.class,
      InitialValueProperty.class,
      BodyHashProperty.class
    )
    .map(PropertyKeys::get)
    .collect(Collectors.toSet());

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

  private static String propertyToValueString(@NotNull Property property) {
    if (ABBREVIATED_PROPERTIES.contains(property.getKey())) {
      return "";
    }
    return ": " + property;
  }

  public boolean containsProperty(@NotNull Class<? extends Property> propertyClass) {
    return containsKey(PropertyKeys.get(propertyClass));
  }

  public PropertyMapDiff diff(@NotNull Map<String, Property> newProperties) {
    var overlay = newProperties.values().stream()
      .filter(p -> Options.fromNullable(get(p.getKey()))
        .map(Property::value)
        .map(v -> !v.equals(p.value()))
        .getOrElse(true)
      )
      .collect(PropertyMap.collectProperties());
    var removedProperties = values().stream()
      .filter(p -> !newProperties.containsKey(p.getKey()))
      .collect(PropertyMap.collectProperties());
    return new PropertyMapDiff(overlay, removedProperties);
  }

  public PropertyMapDiff diff(@NotNull PropertyIndexable newProperties) {
    return diff(newProperties.getProperties());
  }

  public <T extends Property> Option<T> get(Class<T> propertyClass) {
    return Options.fromNullable(super.get(PropertyKeys.get(propertyClass))).narrow(propertyClass);
  }

  public <T> T getOrDefault(Class<? extends SimpleProperty<T>> propertyClass, T defaultValue) {
    return get(propertyClass).map(SimpleProperty::value).getOrElse(defaultValue);
  }

  @CanIgnoreReturnValue
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

  @Override
  public String toString() {
    return "{ %s }".formatted(values().stream()
                                .map(p -> "%s%s".formatted(p.getKey(), propertyToValueString(p)))
                                .collect(Collectors.joining(", ")));
  }

  public record PropertyMapDiff(PropertyMap overlay, PropertyMap removedProperties) {

    public Map<String, Property> coalesce() {
      Map<String, Property> result = new HashMap<>();
      overlay.values().forEach(p -> result.put(p.getKey(), p));
      removedProperties.values().forEach(p -> result.put(p.getKey(), null));
      return result;
    }

    public boolean containsProperty(@NotNull Class<? extends Property> propertyClass) {
      return overlay.containsProperty(propertyClass) || removedProperties.containsProperty(propertyClass);
    }

    public boolean isEmpty() {
      return overlay.isEmpty() && removedProperties.isEmpty();
    }

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

    public <E, T extends Collection<E>> Builder property(@NonNull Function<T, TypedProperty<T>> propertyMapper,
                                                         @NotNull T collection) {
      return property(propertyMapper, Options.fromSized(collection));
    }

    public <T> Builder property(@NonNull Function<T, TypedProperty<T>> propertyMapper, @NotNull Option<T> value) {
      return value.fold(v -> property(propertyMapper.apply(v)), () -> this);
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
