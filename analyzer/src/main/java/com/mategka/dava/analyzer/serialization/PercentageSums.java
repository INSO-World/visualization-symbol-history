package com.mategka.dava.analyzer.serialization;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class PercentageSums {

  public <K> Map<K, @NotNull Integer> getPercentages(Map<K, @NotNull Integer> values, int sum) {
    Map<K, @NotNull Integer> result = new HashMap<>();
    Map<K, @NotNull Double> offsets = new HashMap<>();
    int remaining = 100;
    for (var entry : values.entrySet()) {
      var exact = entry.getValue() * 100D / sum;
      var floored = (int) exact;
      result.put(entry.getKey(), floored);
      offsets.put(entry.getKey(), exact - floored);
      remaining -= floored;
    }
    assert remaining < values.size();
    List<K> keysToIncrement = values.keySet().stream()
      .sorted((a, b) -> offsets.get(b).compareTo(offsets.get(a)))
      .limit(remaining)
      .toList();
    for (K key : keysToIncrement) {
      result.computeIfPresent(key, (_k, v) -> v + 1);
    }
    return result;
  }

}
