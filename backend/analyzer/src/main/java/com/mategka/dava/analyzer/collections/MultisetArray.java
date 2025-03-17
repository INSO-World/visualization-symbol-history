package com.mategka.dava.analyzer.collections;

import com.mategka.dava.analyzer.extension.CollectorsX;
import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.option.Options;

import com.google.common.collect.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class MultisetArray<V> implements Multimap<Integer, V> {

  Multiset<V>[] values;

  private MultisetArray(int length) {
    //noinspection unchecked
    values = IntStream.range(0, length).mapToObj(_i -> HashMultiset.create()).toArray(Multiset[]::new);
  }

  public static <V> MultisetArray<V> create(int length) {
    return new MultisetArray<>(length);
  }

  @Override
  public int size() {
    return multisets().mapToInt(Multiset::size).sum();
  }

  public int length() {
    return values.length;
  }

  private Stream<Multiset<V>> multisets() {
    return Arrays.stream(values);
  }

  @Override
  public boolean isEmpty() {
    return multisets().allMatch(Multiset::isEmpty);
  }

  private Option<Integer> integerKey(Object key) {
    return Options.fromNullable(key).narrow(Number.class).map(Number::intValue);
  }

  @Override
  public boolean containsKey(@Nullable Object key) {
    return integerKey(key).map(this::containsKey).getOrElse(false);
  }

  public boolean containsKey(Integer key) {
    return key != null && 0 <= key && key < values.length;
  }

  @Override
  public boolean containsValue(@Nullable Object value) {
    //noinspection SuspiciousMethodCalls
    return multisets().anyMatch(m -> m.contains(value));
  }

  @Override
  public boolean containsEntry(@Nullable Object key, @Nullable Object value) {
    return integerKey(key).map(i -> containsEntry(i, value)).getOrElse(false);
  }

  public boolean containsEntry(@NotNull Integer key, @Nullable Object value) {
    //noinspection SuspiciousMethodCalls
    return Options.when(containsKey(key), () -> values[key].contains(value)).getOrElse(false);
  }

  @Override
  public boolean put(Integer key, V value) {
    return key != null && values[key].add(value);
  }

  @Override
  public boolean remove(@Nullable Object key, @Nullable Object value) {
    return integerKey(key).map(i -> remove(i, value)).getOrElse(false);
  }

  public boolean remove(Integer key, @Nullable Object value) {
    //noinspection SuspiciousMethodCalls
    return key != null && values[key].remove(value);
  }

  @Override
  public boolean putAll(Integer key, @Nullable Iterable<? extends V> values) {
    if (!containsKey(key) || values == null) {
      return false;
    }
    var multiset = this.values[key];
    return Iterables.addAll(multiset, values);
  }

  @Override
  public boolean putAll(@NotNull Multimap<? extends Integer, ? extends V> multimap) {
    //noinspection unchecked
    var actualMultimap = (Multimap<Integer, ? extends V>) multimap;
    var validKeys = actualMultimap.keySet().stream().filter(this::containsKey).toList();
    return validKeys.stream()
      .map(k -> putAll(k, actualMultimap.get(k)))
      .reduce(false, (a, b) -> a || b);
  }

  @Override
  public @NotNull Multiset<V> replaceValues(Integer key, @NotNull Iterable<? extends V> values) {
    if (key == null) {
      return ImmutableMultiset.of();
    }
    var oldValues = this.values[key];
    this.values[key] = HashMultiset.create(values);
    return oldValues;
  }

  @Override
  public @NotNull Multiset<V> removeAll(@Nullable Object key) {
    return integerKey(key).map(this::removeAll).getOrCompute(ImmutableMultiset::of);
  }

  public Multiset<V> removeAll(Integer key) {
    if (key == null) {
      return ImmutableMultiset.of();
    }
    var oldValues = this.values[key];
    this.values[key] = HashMultiset.create();
    return oldValues;
  }

  @Override
  public void clear() {
    multisets().forEach(Multiset::clear);
  }

  @Override
  public @NotNull Multiset<V> get(Integer key) {
    if (key == null) {
      return HashMultiset.create();
    }
    return values[key];
  }

  @Override
  public @NotNull Set<Integer> keySet() {
    return keyStream().collect(Collectors.toSet());
  }

  @Override
  public @NotNull Multiset<Integer> keys() {
    return entries().stream()
      .map(Map.Entry::getKey)
      .collect(ImmutableMultiset.toImmutableMultiset());
  }

  @Override
  public @NotNull Collection<V> values() {
    return multisets().flatMap(m -> m.elementSet().stream()).toList();
  }

  @Override
  public @NotNull Collection<Map.Entry<Integer, V>> entries() {
    return keyStream()
      .flatMap(k -> values[k].stream().map(v -> Map.entry(k, v)))
      .toList();
  }

  private Stream<Integer> keyStream() {
    return IntStream.range(0, values.length).boxed();
  }

  @Override
  public @NotNull Map<Integer, Collection<V>> asMap() {
    return keyStream()
      .map(k -> Map.entry(k, (Collection<V>) values[k]))
      .collect(CollectorsX.entriesToMap());
  }

}
