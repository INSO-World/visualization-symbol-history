package com.mategka.dava.analyzer.collections;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class IndexMap<K, V> implements Map<K, V> {

  @Delegate
  Map<K, V> map;

  Function<V, K> keyExtractor;

  public IndexMap(Supplier<Map<K, V>> mapSupplier, Function<V, K> keyExtractor) {
    this(mapSupplier.get(), keyExtractor);
  }

  public IndexMap(Function<V, K> keyExtractor) {
    this(HashMap::new, keyExtractor);
  }

  @Override
  public boolean containsValue(Object value) {
    try {
      //noinspection unchecked
      return containsKey(keyExtractor.apply((V) value));
    } catch (ClassCastException e) {
      return false;
    }
  }

  @Override
  public @Nullable V put(K key, V value) {
    var actualKey = keyExtractor.apply(value);
    if (!Objects.equals(key, actualKey)) {
      throw new IllegalArgumentException("Supplied key did not match key from value");
    }
    return map.put(actualKey, value);
  }

  public @Nullable V put(V value) {
    return map.put(keyExtractor.apply(value), value);
  }

  public V removeByValue(V value) {
    return map.remove(keyExtractor.apply(value));
  }

  @Override
  public void putAll(@NotNull Map<? extends K, ? extends V> m) {
    var mismatches = m.entrySet().stream()
      .anyMatch(e -> !Objects.equals(e.getKey(), keyExtractor.apply(e.getValue())));
    if (mismatches) {
      throw new IllegalArgumentException("Supplied key did not match key from value");
    }
    map.putAll(m);
  }

}
