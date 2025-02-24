package com.mategka.dava.analyzer.extension;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
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

  public <A, B> Collector<Pair<A, B>, ?, BiMap<A, B>> toBiMap() {
    return Collectors.toMap(Pair::left, Pair::right, (a, b) -> b, HashBiMap::create);
  }

}
