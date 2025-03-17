package com.mategka.dava.analyzer.wip;

import com.mategka.dava.analyzer.extension.Pair;
import com.mategka.dava.analyzer.extension.option.Option;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public final class Maximum<V, K extends Comparable<K>> {

  private final Function<V, @NotNull K> keyFunction;

  @Getter
  private Option<Pair<V, K>> maximum = Option.None();

  public Maximum(Function<V, @NotNull K> keyFunction) {
    this.keyFunction = keyFunction;
  }

  public void clear() {
    maximum = Option.None();
  }

  public @NotNull Option<@NotNull K> getMaximumKey() {
    return maximum.map(Pair::right);
  }

  public @NotNull Option<V> getMaximumValue() {
    return maximum.map(Pair::left);
  }

  public boolean update(@NotNull V value) {
    var key = keyFunction.apply(value);
    if (maximum.isNone() || key.compareTo(maximum.getOrThrow().right()) > 0) {
      maximum = Option.Some(Pair.of(value, key));
      return true;
    }
    return false;
  }

}
