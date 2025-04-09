package com.mategka.dava.analyzer.extension;

import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.stream.AnStream;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@UtilityClass
public class CollectionsX {

  public <T, U extends T> Option<U> firstOfType(SequencedCollection<T> collection, Class<U> clazz) {
    return AnStream.from(collection).allow(clazz).findFirstAsOption();
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

  @UnmodifiableView
  public <K, V> Collection<V> unmodifiableCollection(Map<? super K, ? extends V> map, K key) {
    return new AbstractCollection<>() {
      @Override
      public @NotNull Iterator<V> iterator() {
        V value = map.get(key);
        if (!map.containsKey(key)) {
          return Collections.emptyIterator();
        } else {
          return Collections.singleton(value).iterator();
        }
      }

      @Override
      public int size() {
        return map.containsKey(key) ? 1 : 0;
      }

      @Override
      public boolean contains(Object o) {
        return map.containsKey(key) && Objects.equals(map.get(key), o);
      }
    };
  }

}
