package com.mategka.dava.analyzer.extension;

import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@UtilityClass
public class CollectionsX {

  public <T> boolean onlyElementMatches(Collection<T> collection, Predicate<T> predicate) {
    return collection.size() == 1 && predicate.test(collection.iterator().next());
  }

  public <T, K> Map<K, T> groupBy(Collection<T> collection, Function<? super T, K> keyFn) {
    return collection.stream().collect(Collectors.toMap(keyFn, Function.identity()));
  }

  public <T, U extends T> Optional<U> firstOfType(SequencedCollection<T> collection, Class<U> clazz) {
    return collection.stream().mapMulti(StreamsX.onlyOfType(clazz)).findFirst();
  }

  public <T, U extends T> Optional<U> lastOfType(SequencedCollection<T> collection, Class<U> clazz) {
    return firstOfType(collection.reversed(), clazz);
  }

}
