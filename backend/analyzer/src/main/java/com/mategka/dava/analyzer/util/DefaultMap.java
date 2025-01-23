package com.mategka.dava.analyzer.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class DefaultMap<K, V> implements Map<K, V> {

  @Delegate
  Map<K, V> map;

  Function<K, V> defaultSupplier;

  public DefaultMap(Supplier<Map<K, V>> mapSupplier, Function<K, V> defaultSupplier) {
    this(mapSupplier.get(), defaultSupplier);
  }

  @SuppressWarnings("unchecked")
  @Override
  public V get(Object key) {
    return map.computeIfAbsent((K) key, defaultSupplier);
  }

}
