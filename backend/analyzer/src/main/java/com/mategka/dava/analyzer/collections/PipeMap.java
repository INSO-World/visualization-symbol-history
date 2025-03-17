package com.mategka.dava.analyzer.collections;

import com.mategka.dava.analyzer.extension.option.Option;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;

@UnmodifiableView
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PipeMap<K, V> implements Map<K, V> {

  private final List<Map<?, ?>> maps;

  public static <K, K2, V> PipeMap<K, V> of(Map<? super K, K2> map1, Map<? super K2, V> map2) {
    return new PipeMap<>(List.of(map1, map2));
  }

  public static <K, K2, K3, V> PipeMap<K, V> of(Map<? super K, K2> map1, Map<? super K2, K3> map2,
                                                Map<? super K3, V> map3) {
    return new PipeMap<>(List.of(map1, map2, map3));
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("PipeMap is immutable");
  }

  @Override
  public boolean containsKey(Object key) {
    //noinspection SuspiciousMethodCalls
    return keySet().contains(key);
  }

  @Override
  public boolean containsValue(Object value) {
    //noinspection SuspiciousMethodCalls
    return values().contains(value);
  }

  @Override
  public @NotNull Set<Entry<K, V>> entrySet() {
    return Set.of();
  }

  @Override
  public V get(Object key) {
    //noinspection unchecked
    return getStrict((K) key).getOrNull();
  }

  public Option<V> getStrict(K key) {
    Object currentKey = key;
    for (Map<?, ?> map : maps) {
      if (!map.containsKey(currentKey)) {
        return Option.None();
      }
      currentKey = map.get(currentKey);
    }
    //noinspection unchecked
    return Option.Some((V) currentKey);
  }

  @Override
  public boolean isEmpty() {
    return keySet().isEmpty();
  }

  @Override
  public @NotNull Set<K> keySet() {
    return Set.of();
  }

  @Override
  public @Nullable V put(K key, V value) {
    throw new UnsupportedOperationException("PipeMap is immutable");
  }

  @Override
  public void putAll(@NotNull Map<? extends K, ? extends V> m) {
    throw new UnsupportedOperationException("PipeMap is immutable");
  }

  @Override
  public V remove(Object key) {
    throw new UnsupportedOperationException("PipeMap is immutable");
  }

  @Override
  public int size() {
    return keySet().size();
  }

  @Override
  public @NotNull Collection<V> values() {
    return List.of();
  }

}
