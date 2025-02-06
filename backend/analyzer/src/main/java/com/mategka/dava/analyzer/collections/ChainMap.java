package com.mategka.dava.analyzer.collections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@UnmodifiableView
public class ChainMap<K, V> implements Map<K, V> {

  private final List<Map<? extends K, ? extends V>> maps;

  @SafeVarargs
  public ChainMap(Map<? extends K, ? extends V>... maps) {
    this.maps = Arrays.asList(maps);
  }

  public static <K1, K2, V> V getOnce(Map<? super K1, K2> map1, Map<? super K2, V> map2, K1 key) {
    if (!map1.containsKey(key)) {
      return null;
    }
    return map2.get(map1.get(key));
  }

  public static <K1, K2, K3, V> V getOnce(Map<? super K1, K2> map1, Map<? super K2, K3> map2, Map<? super K3, V> map3,
                                          K1 key) {
    if (!map1.containsKey(key)) {
      return null;
    }
    var key2 = map1.get(key);
    if (!map2.containsKey(key2)) {
      return null;
    }
    return map3.get(map2.get(key2));
  }

  @Override
  public int size() {
    return keySet().size();
  }

  @Override
  public boolean isEmpty() {
    return maps.stream().allMatch(Map::isEmpty);
  }

  @Override
  public boolean containsKey(Object key) {
    return maps.stream().anyMatch(map -> map.containsKey(key));
  }

  public boolean containsKeyStrict(K key) {
    return containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return maps.stream().anyMatch(map -> map.containsValue(value));
  }

  public boolean containsValueStrict(V value) {
    return containsValue(value);
  }

  @Override
  public V get(Object key) {
    return getInternal(key).orElse(null);
  }

  public Optional<V> getStrict(K key) {
    return getInternal(key);
  }

  private Optional<V> getInternal(Object key) {
    //noinspection SuspiciousMethodCalls
    return maps.stream()
      .filter(map -> map.containsKey(key))
      .findFirst()
      .map(map -> map.get(key));
  }

  @Override
  public @Nullable V put(K key, V value) {
    throw new UnsupportedOperationException("ChainMap is immutable");
  }

  @Override
  public V remove(Object key) {
    throw new UnsupportedOperationException("ChainMap is immutable");
  }

  @Override
  public void putAll(@NotNull Map<? extends K, ? extends V> m) {
    throw new UnsupportedOperationException("ChainMap is immutable");
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("ChainMap is immutable");
  }

  @Override
  public @NotNull Set<K> keySet() {
    return maps.stream()
      .map(Map::keySet)
      .flatMap(Collection::stream)
      .collect(Collectors.toCollection(HashSet::new));
  }

  @Override
  public @NotNull Collection<V> values() {
    return keySet().stream()
      .map(this::get)
      .collect(Collectors.toList());
  }

  @Override
  public @NotNull Set<Map.Entry<K, V>> entrySet() {
    return keySet().stream()
      .map(key -> new Entry<>(key, get(key)))
      .collect(Collectors.toSet());
  }

  @Override
  public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    throw new UnsupportedOperationException("ChainMap is immutable");
  }

  @Override
  public @Nullable V putIfAbsent(K key, V value) {
    throw new UnsupportedOperationException("ChainMap is immutable");
  }

  @Override
  public boolean remove(Object key, Object value) {
    throw new UnsupportedOperationException("ChainMap is immutable");
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    throw new UnsupportedOperationException("ChainMap is immutable");
  }

  @Override
  public @Nullable V replace(K key, V value) {
    throw new UnsupportedOperationException("ChainMap is immutable");
  }

  @Override
  public V computeIfAbsent(K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
    throw new UnsupportedOperationException("ChainMap is immutable");
  }

  @Override
  public V computeIfPresent(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    throw new UnsupportedOperationException("ChainMap is immutable");
  }

  @Override
  public V compute(K key, @NotNull BiFunction<? super K, ? super @Nullable V, ? extends V> remappingFunction) {
    throw new UnsupportedOperationException("ChainMap is immutable");
  }

  @Override
  public V merge(K key, @NotNull V value, @NotNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
    throw new UnsupportedOperationException("ChainMap is immutable");
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ChainMap<?, ?> chainMap)) return false;
    if (maps.size() != chainMap.maps.size()) return false;
    return IntStream.range(0, maps.size())
      .allMatch(i -> maps.get(i) == chainMap.maps.get(i));
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(maps);
  }

  public static class Entry<K, V> implements Map.Entry<K, V> {

    private final K key;
    private final V value;

    protected Entry(final K key, final V value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return value;
    }

    @Override
    public V setValue(V value) {
      throw new UnsupportedOperationException("ChainMap.Entry is immutable");
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Entry<?, ?> entry)) return false;
      return Objects.equals(key, entry.key) && Objects.equals(value, entry.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(key, value);
    }

  }

}
