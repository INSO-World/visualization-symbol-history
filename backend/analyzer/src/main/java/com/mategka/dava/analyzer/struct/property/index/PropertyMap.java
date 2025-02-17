package com.mategka.dava.analyzer.struct.property.index;

import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.struct.property.Property;
import com.mategka.dava.analyzer.struct.property.SimpleProperty;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class PropertyMap extends HashMap<String, Property> {

  public static PropertyMap of(Property... properties) {
    var result = new PropertyMap();
    for (Property property : properties) {
      result.put(property);
    }
    return result;
  }

  public static <T> Collector<T, ?, PropertyMap> collector(
    @NotNull Function<? super T, String> keyFunction,
    Function<? super T, ? extends Property> valueFunction
  ) {
    return Collectors.toMap(keyFunction, valueFunction, (older, newer) -> newer, PropertyMap::new);
  }

  public static <T extends Property> Collector<T, ?, PropertyMap> collectProperties() {
    return collector(Property::getKey, Function.identity());
  }

  public static <T extends Map.Entry<String, Property>> Collector<T, ?, PropertyMap> collectEntries() {
    return collector(Map.Entry::getKey, Map.Entry::getValue);
  }

  public <T extends Property> Option<T> get(Class<T> propertyClass) {
    return Option.fromNullable(super.get(PropertyKeys.get(propertyClass))).narrow(propertyClass);
  }

  public <T> T getOrDefault(Class<? extends SimpleProperty<T>> propertyClass, T defaultValue) {
    return get(propertyClass).map(SimpleProperty::value).getOrElse(defaultValue);
  }

  public Property put(Property property) {
    return super.put(property.getKey(), property);
  }

  public Property putIfAbsent(Property property) {
    return super.putIfAbsent(property.getKey(), property);
  }

  public void putAll(Collection<? extends Property> c) {
    super.putAll(c.stream().collect(collectProperties()));
  }

}
