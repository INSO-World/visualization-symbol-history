package com.mategka.dava.analyzer.extension;

import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
public class SetsX {

  public <E, T> Set<T> keysOf(Collection<E> collection, Function<E, T> keyMapper) {
    return collection.stream().map(keyMapper).collect(Collectors.toSet());
  }

}
