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
import java.util.function.Function;

@EqualsAndHashCode
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CountingMap<K> implements Map<K, @NotNull Integer> {

  private static final Function<? super @NotNull Integer, @NotNull Integer> INCREMENT = v -> v + 1;
  private static final Function<? super @NotNull Integer, @NotNull Integer> DECREMENT = v -> v - 1;

  @Delegate
  Map<K, @NotNull Integer> map = new HashMap<>();

  public void decrement(K key) {
    update(key, DECREMENT);
  }

  @CheckReturnValue
  public int decrementAndGet(K key) {
    return updateAndGet(key, DECREMENT);
  }

  @CheckReturnValue
  public int getAndDecrement(K key) {
    return getAndUpdate(key, DECREMENT);
  }

  @CheckReturnValue
  public int getAndIncrement(K key) {
    return getAndUpdate(key, INCREMENT);
  }

  public void increment(K key) {
    update(key, INCREMENT);
  }

  @CheckReturnValue
  public int incrementAndGet(K key) {
    return updateAndGet(key, INCREMENT);
  }

  @CheckReturnValue
  private int getAndUpdate(K key, Function<? super @NotNull Integer, @NotNull Integer> updateFn) {
    return Options.fromNullable(map.put(key, updateFn.apply(map.getOrDefault(key, 0))))
      .getOrElse(0);
  }

  private void update(K key, Function<? super @NotNull Integer, @NotNull Integer> updateFn) {
    map.put(key, updateFn.apply(map.getOrDefault(key, 0)));
  }

  @CheckReturnValue
  private int updateAndGet(K key, Function<? super @NotNull Integer, @NotNull Integer> updateFn) {
    return map.compute(key, (_k, oldValue) -> updateFn.apply(oldValue));
  }

}
