package com.mategka.dava.analyzer.extension;

import com.mategka.dava.analyzer.util.AbstractPath;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@UtilityClass
public class Collectors2 {

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

  public <K, V> Collector<K, ?, Map<K, V>> toMap(Function<K, V> mapper) {
    return Collectors.toMap(Function.identity(), mapper);
  }

}
