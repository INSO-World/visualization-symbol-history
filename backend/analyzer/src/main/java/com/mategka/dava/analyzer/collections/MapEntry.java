package com.mategka.dava.analyzer.collections;

import lombok.Value;

import java.util.Map;
import java.util.Objects;

@Value(staticConstructor = "of")
public class MapEntry<K, V> implements Map.Entry<K, V> {

  K key;
  V value;

  @Override
  public V setValue(V value) {
    throw new UnsupportedOperationException("Entry is immutable");
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Map.Entry<?, ?> entry)) return false;
    return Objects.equals(key, entry.getKey()) && Objects.equals(value, entry.getValue());
  }

  @Override
  public int hashCode() {
    return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
  }

}
