package com.mategka.dava.analyzer.struct.property.index;

import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.option.Options;
import com.mategka.dava.analyzer.struct.property.*;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PropertyMap extends HashMap<String, @NotNull Property> implements PropertyIndexable {

  private static final Set<String> ABBREVIATED_PROPERTIES = Stream.of(
      SpoonPathProperty.class,
      InitialValueProperty.class,
      BodyHashProperty.class,
      PathProperty.class
    )
    .map(PropertyKeys::get)
    .collect(Collectors.toSet());

  @Serial
  private static final long serialVersionUID = -958149528863310727L;

  public PropertyMap() {
    super();
  }

  public PropertyMap(@NotNull final Map<String, @NotNull Property> properties) {
    super(properties);
  }

  public static @NotNull PropertyMap of(Property @NotNull ... properties) {
    var result = new PropertyMap();
    for (Property property : properties) {
      result.put(property);
    }
    return result;
  }

  @Contract(" -> new")
  public static @NotNull Builder builder() {
    return new Builder();
  }

  @Contract(value = " -> new", pure = true)
  public static <T extends Map.Entry<String, Property>> @NotNull Collector<T, ?, PropertyMap> collectEntries() {
    return collector(Map.Entry::getKey, Map.Entry::getValue);
  }

  @Contract(" -> new")
  public static <T extends Property> @NotNull Collector<T, ?, PropertyMap> collectProperties() {
    return collector(Property::getKey, Function.identity());
  }

  @Contract(value = "_, _ -> new", pure = true)
  public static <T> @NotNull Collector<T, ?, PropertyMap> collector(
    @NotNull Function<? super T, String> keyFunction,
    Function<? super T, ? extends Property> valueFunction
  ) {
    return Collectors.toMap(keyFunction, valueFunction, (older, newer) -> newer, PropertyMap::new);
  }

  private static @NotNull String propertyToValueString(@Nullable Property property) {
    if (property == null) {
      return "<removed>";
    }
    if (ABBREVIATED_PROPERTIES.contains(property.getKey())) {
      return "";
    }
    return ": " + property;
  }

  public void applyUpdate(@NotNull Map<String, @Nullable Property> propertyUpdates) {
    for (var propertyUpdate : propertyUpdates.entrySet()) {
      var key = propertyUpdate.getKey();
      var value = propertyUpdate.getValue();
      if (value != null) {
        put(key, value);
      } else {
        remove(key);
      }
    }
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

  @Override
  public @NotNull Map<String, Property> getProperties() {
    return this;
  }

  @Override
  public Property put(@NotNull String key, @Nullable Property value) {
    if (value != null && !key.equals(value.getKey())) {
      throw new IllegalArgumentException("Properties can only be added with their own key");
    }
    //noinspection DataFlowIssue
    return super.put(key, value);
  }

  @CanIgnoreReturnValue
  public Property put(Property property) {
    return super.put(property.getKey(), property);
  }

  public void putAll(@NotNull Collection<? extends Property> c) {
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
    return "{ %s }".formatted(entrySet().stream()
                                .map(e -> "%s%s".formatted(e.getKey(), propertyToValueString(e.getValue())))
                                .collect(Collectors.joining(", ")));
  }

  public record PropertyMapDiff(PropertyMap overlay, PropertyMap removedProperties) {

    public Map<String, Property> coalesce() {
      Map<String, Property> result = overlay;
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

    @SuppressWarnings("unchecked")
    public <E extends Serializable, T extends Collection<E>> Builder property(
      @NonNull Function<? super T, ? extends CollectionProperty<E>> propertyMapper,
      @NotNull T collection) {
      return property((Function<? super T, ? extends TypedProperty<T>>) propertyMapper, Options.fromSized(collection));
    }

    public <T> Builder property(@NonNull Function<? super T, ? extends TypedProperty<T>> propertyMapper,
                                @NotNull Option<T> value) {
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
