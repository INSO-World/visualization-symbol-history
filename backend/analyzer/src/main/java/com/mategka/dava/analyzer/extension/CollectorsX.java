package com.mategka.dava.analyzer.extension;

import com.mategka.dava.analyzer.extension.struct.Pair;

import com.google.common.collect.*;
import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@UtilityClass
public class CollectorsX {

  public <T> Collector<T, ?, String> commaSeparated() {
    return Collectors.mapping(Objects::toString, Collectors.joining(", "));
  }

  public <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>> entriesToMap() {
    return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
  }

  public <K, V> Collector<V, ?, Map<K, V>> mapToKey(Function<V, K> mapper) {
    return Collectors.toMap(mapper, Function.identity());
  }

  public <K, V> Collector<K, ?, Map<K, V>> mapToValue(Function<K, V> mapper) {
    return Collectors.toMap(Function.identity(), mapper);
  }

  public <K, V> Collector<Pair<K, V>, ?, Map<K, V>> pairsToMap() {
    return Collectors.toMap(Pair::left, Pair::right);
  }

  public <K, V> Collector<Pair<K, V>, ?, Map<K, V>> pairsToMap2() {
    return Collectors.toMap(Pair::left, Pair::right, (a, b) -> b);
  }

  public <K, V, M extends Map<K, V>> Collector<Pair<K, V>, ?, M> pairsToMutableMap(Supplier<M> mapFactory) {
    return Collectors.toMap(Pair::left, Pair::right, (a, b) -> b, mapFactory);
  }

  public <A, B> Collector<Pair<A, B>, ?, BiMap<A, B>> toBiMap() {
    return toBiMap(HashBiMap::create);
  }

  public <A, B> Collector<Pair<A, B>, ?, BiMap<A, B>> toBiMap(Supplier<BiMap<A, B>> mapSupplier) {
    return Collectors.toMap(Pair::left, Pair::right, (a, b) -> b, mapSupplier);
  }

  public <T, K, V> Collector<T, ?, Multimap<K, V>> toMultimap(
    Function<? super T, K> keyMapper,
    Function<? super T, V> valueMapper
  ) {
    return Collector.of(
      HashMultimap::create,
      (m, t) -> m.put(keyMapper.apply(t), valueMapper.apply(t)),
      (m1, m2) -> {
        m1.putAll(m2);
        return m1;
      },
      Collector.Characteristics.UNORDERED
    );
  }

}
