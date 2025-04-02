package com.mategka.dava.analyzer.collections;

import com.mategka.dava.analyzer.extension.option.Options;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@EqualsAndHashCode
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CountingMap<K> implements Map<K, @NotNull Integer> {

  @Delegate
  Map<K, @NotNull Integer> map = new HashMap<>();

  public void increment(K key) {
    map.put(key, map.getOrDefault(key, 0) + 1);
  }

  @CheckReturnValue
  public int getAndIncrement(K key) {
    return Options.fromNullable(map.put(key, map.getOrDefault(key, 0) + 1))
      .getOrElse(0);
  }

  @CheckReturnValue
  public int incrementAndGet(K key) {
    return getAndIncrement(key) + 1;
  }

}
