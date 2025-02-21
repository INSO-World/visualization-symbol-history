package com.mategka.dava.analyzer.extension;

import com.mategka.dava.analyzer.util.AbstractPath;

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

  private static final String GROUP_SEPARATOR = "\u001d";

  public <T> Collector<T, ?, Pair<T, T>> toPair() {
    //noinspection SequencedCollectionMethodCanBeUsed
    return Collectors.collectingAndThen(Collectors.toList(), (list) -> Pair.of(list.get(0), list.get(1)));
  }

  public Collector<String, ?, AbstractPath> toPath() {
    return Collectors.collectingAndThen(Collectors.toList(), AbstractPath::of);
  }

  public <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>> entriesToMap() {
    return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
  }

  public <T> Collector<T, ?, String> commaSeparated() {
    return Collectors.mapping(Objects::toString, Collectors.joining(", "));
  }

  public <K, V> Collector<Pair<K, V>, ?, Map<K, V>> pairsToMap() {
    return Collectors.toMap(Pair::left, Pair::right);
  }

  public <K, V> Collector<K, ?, Map<K, V>> mapToValue(Function<K, V> mapper) {
    return Collectors.toMap(Function.identity(), mapper);
  }

  public <K, V> Collector<V, ?, Map<K, V>> mapToKey(Function<V, K> mapper) {
    return Collectors.toMap(mapper, Function.identity());
  }

  public <A, B> Collector<Pair<A, B>, ?, BiMap<A, B>> toBiMap() {
    return Collectors.toMap(Pair::left, Pair::right, (a, b) -> b, HashBiMap::create);
  }

}
