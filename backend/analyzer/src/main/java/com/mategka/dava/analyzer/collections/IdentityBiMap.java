package com.mategka.dava.analyzer.collections;

import com.mategka.dava.analyzer.extension.option.Options;
import com.mategka.dava.analyzer.extension.stream.AnStream;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class IdentityBiMap<K, V> implements BiMap<K, V> {

  final BiMap<IdentityValue<K>, IdentityValue<V>> map;
  IdentityBiMap<V, K> inverseMap = null;

  public IdentityBiMap() {
    map = HashBiMap.create();
  }

  public IdentityBiMap(Supplier<BiMap<?, ?>> mapSupplier) {
    //noinspection unchecked
    map = (BiMap<IdentityValue<K>, IdentityValue<V>>) mapSupplier.get();
  }

  private IdentityBiMap(BiMap<IdentityValue<K>, IdentityValue<V>> map) {
    this.map = map;
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(new IdentityValue<>(key));
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(new IdentityValue<>(value));
  }

  @Override
  @Unmodifiable
  public @NotNull Set<Entry<K, V>> entrySet() {
    return AnStream.from(map.entrySet())
      .map(e -> Map.entry(e.getKey().value(), e.getValue().value()))
      .toSet();
  }

  @Override
  public @Nullable V forcePut(K key, V value) {
    return unwrapOrNull(map.forcePut(new IdentityValue<>(key), new IdentityValue<>(value)));
  }

  @Override
  public V get(Object key) {
    return unwrapOrNull(map.get(key));
  }

  @Override
  public @NotNull IdentityBiMap<V, K> inverse() {
    if (inverseMap == null) {
      inverseMap = new IdentityBiMap<>(map.inverse());
    }
    return inverseMap;
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  @Unmodifiable
  public @NotNull Set<K> keySet() {
    return AnStream.from(map.keySet()).map(IdentityValue::value).toSet();
  }

  @Override
  public @Nullable V put(K key, V value) {
    return unwrapOrNull(map.put(new IdentityValue<>(key), new IdentityValue<>(value)));
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    //noinspection unchecked
    this.map.putAll((Map<? extends IdentityValue<K>, ? extends IdentityValue<V>>) map.entrySet().stream()
      .collect(Collectors.toMap(
        e -> new IdentityValue<>(e.getKey()),
        e -> new IdentityValue<>(e.getValue()),
        (a, b) -> b,
        HashMap::new
      ))
    );
  }

  @Override
  public V remove(Object key) {
    return unwrapOrNull(map.remove(new IdentityValue<>(key)));
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  @Unmodifiable
  public @NotNull Set<V> values() {
    return AnStream.from(map.values()).map(IdentityValue::value).toSet();
  }

  private <T> T unwrapOrNull(@Nullable IdentityValue<T> value) {
    return Options.fromNullable(value)
      .map(IdentityValue::value)
      .getOrNull();
  }

  @Accessors(fluent = true)
  @Getter
  @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  private static class IdentityValue<T> {

    @NotNull
    T value;

    @Override
    public boolean equals(@Nullable Object o) {
      if (o == this) return true;
      if (!(o instanceof IdentityBiMap.IdentityValue<?> other)) return false;
      return value == other.value;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(value);
    }

    @Override
    public String toString() {
      return Objects.toString(value);
    }

  }

}
