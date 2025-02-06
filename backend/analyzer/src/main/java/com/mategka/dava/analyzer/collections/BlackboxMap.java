package com.mategka.dava.analyzer.collections;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class BlackboxMap<K, V, KK, VV> implements Map<K, V> {

  Map<KK, VV> map;

  Function<K, KK> preTransformer;
  Function<VV, V> postTransformer;
  Function<KK, K> reversePreTransformer;
  Function<V, VV> reversePostTransformer;

  public BlackboxMap(Supplier<Map<KK, VV>> mapSupplier, Function<K, KK> preTransformer, Function<VV, V> postTransformer,
                     Function<KK, K> reversePreTransformer, Function<V, VV> reversePostTransformer) {
    this(mapSupplier.get(), preTransformer, postTransformer, reversePreTransformer, reversePostTransformer);
  }

  public BlackboxMap(Function<K, KK> preTransformer, Function<VV, V> postTransformer,
                     Function<KK, K> reversePreTransformer, Function<V, VV> reversePostTransformer) {
    this(HashMap::new, preTransformer, postTransformer, reversePreTransformer, reversePostTransformer);
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    try {
      //noinspection unchecked
      return map.containsKey(preTransformer.apply((K) key));
    } catch (ClassCastException e) {
      return false;
    }
  }

  @Override
  public boolean containsValue(Object value) {
    try {
      //noinspection unchecked
      return map.containsValue(reversePostTransformer.apply((V) value));
    } catch (ClassCastException e) {
      return false;
    }
  }

  @Override
  public V get(Object key) {
    try {
      //noinspection unchecked
      return postTransform(map.get(preTransformer.apply((K) key)));
    } catch (ClassCastException e) {
      return null;
    }
  }

  private V postTransform(VV value) {
    return value == null ? null : postTransformer.apply(value);
  }

  private VV reversePostTransform(V value) {
    return value == null ? null : reversePostTransformer.apply(value);
  }

  @Override
  public @Nullable V put(K key, V value) {
    return postTransform(map.put(preTransformer.apply(key), reversePostTransformer.apply(value)));
  }

  @Override
  public V remove(Object key) {
    try {
      //noinspection unchecked
      return postTransform(map.remove(preTransformer.apply((K) key)));
    } catch (ClassCastException e) {
      return null;
    }
  }

  @Override
  public void putAll(@NotNull Map<? extends K, ? extends V> m) {
    m.forEach(this::put);
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public @NotNull Set<K> keySet() {
    return map.keySet().stream()
      .map(reversePreTransformer)
      .collect(Collectors.toSet());
  }

  @Override
  public @NotNull Collection<V> values() {
    return map.values().stream()
      .map(postTransformer)
      .toList();
  }

  @Override
  public @NotNull Set<Entry<K, V>> entrySet() {
    return map.entrySet().stream()
      .map(e -> Map.entry(reversePreTransformer.apply(e.getKey()), postTransformer.apply(e.getValue())))
      .collect(Collectors.toSet());
  }

  @Override
  public V getOrDefault(Object key, V defaultValue) {
    try {
      //noinspection unchecked
      return postTransform(map.getOrDefault(preTransformer.apply((K) key), reversePostTransformer.apply(defaultValue)));
    } catch (ClassCastException e) {
      return null;
    }
  }

  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {
    map.forEach((kk, vv) -> action.accept(reversePreTransformer.apply(kk), postTransformer.apply(vv)));
  }

  @Override
  public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    map.replaceAll(
      (kk, vv) -> reversePostTransform(function.apply(reversePreTransformer.apply(kk), postTransformer.apply(vv))));
  }

  @Override
  public @Nullable V putIfAbsent(K key, V value) {
    return postTransform(map.putIfAbsent(preTransformer.apply(key), reversePostTransformer.apply(value)));
  }

  @Override
  public boolean remove(Object key, Object value) {
    try {
      //noinspection unchecked
      return map.remove(preTransformer.apply((K) key), reversePostTransformer.apply((V) value));
    } catch (ClassCastException e) {
      return false;
    }
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    try {
      return map.replace(preTransformer.apply(key), reversePostTransform(oldValue), reversePostTransform(newValue));
    } catch (ClassCastException e) {
      return false;
    }
  }

  @Override
  public @Nullable V replace(K key, V value) {
    try {
      return postTransform(map.replace(preTransformer.apply(key), reversePostTransform(value)));
    } catch (ClassCastException e) {
      return null;
    }
  }

  @Override
  public V computeIfAbsent(K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
    return postTransform(map.computeIfAbsent(
      preTransformer.apply(key), (kk) -> reversePostTransform(
        mappingFunction.apply(reversePreTransformer.apply(kk)))
    ));
  }

  @Override
  public V computeIfPresent(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    return postTransform(map.computeIfPresent(
      preTransformer.apply(key), (kk, vv) -> reversePostTransform(
        remappingFunction.apply(reversePreTransformer.apply(kk), postTransform(vv)))
    ));
  }

  @Override
  public V compute(K key, @NotNull BiFunction<? super K, ? super @Nullable V, ? extends V> remappingFunction) {
    return postTransform(map.compute(
      preTransformer.apply(key), (kk, vv) -> reversePostTransform(
        remappingFunction.apply(reversePreTransformer.apply(kk), postTransform(vv)))
    ));
  }

  @Override
  public V merge(K key, @NotNull V value, @NotNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
    return postTransform(map.merge(
      preTransformer.apply(key), reversePostTransform(value),
      (vv1, vv2) -> reversePostTransform(
        remappingFunction.apply(postTransform(vv1), postTransform(vv2)))
    ));
  }

}
