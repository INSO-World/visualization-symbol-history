package com.mategka.dava.analyzer.extension;

import com.mategka.dava.analyzer.extension.option.Option;

import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@UtilityClass
public class CollectionsX {

  public <T, U extends T> Option<U> firstOfType(SequencedCollection<T> collection, Class<U> clazz) {
    return AnStream.from(collection).narrow(clazz).findFirstAsOption();
  }

  public <T, K> Map<K, T> groupBy(Collection<T> collection, Function<? super T, K> keyFn) {
    return collection.stream().collect(Collectors.toMap(keyFn, Function.identity()));
  }

  public <T, U extends T> Option<U> lastOfType(SequencedCollection<T> collection, Class<U> clazz) {
    return firstOfType(collection.reversed(), clazz);
  }

  public <T> boolean onlyElementMatches(Collection<T> collection, Predicate<T> predicate) {
    return collection.size() == 1 && predicate.test(collection.iterator().next());
  }

}
