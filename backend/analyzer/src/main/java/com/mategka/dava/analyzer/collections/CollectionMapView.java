package com.mategka.dava.analyzer.collections;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@UnmodifiableView
public class CollectionMapView<T, K, V> extends AbstractMap<K, V> {

  Collection<? extends T> collection;

  Function<T, K> keyExtractor;
  Function<T, V> valueExtractor;

  @Override
  public @NotNull Set<Entry<K, V>> entrySet() {
    return collection.stream()
      .map(e -> Map.entry(keyExtractor.apply(e), valueExtractor.apply(e)))
      .collect(Collectors.toSet());
  }

  @Override
  public V get(Object key) {
    return collection.stream()
      .filter(e -> Objects.equals(keyExtractor.apply(e), key))
      .map(valueExtractor)
      .findFirst()
      .orElse(null);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof CollectionMapView<?, ?, ?> that)) return false;
    return collection == that.collection
      && Objects.equals(keyExtractor, that.keyExtractor)
      && Objects.equals(valueExtractor, that.valueExtractor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), collection, keyExtractor, valueExtractor);
  }

}
