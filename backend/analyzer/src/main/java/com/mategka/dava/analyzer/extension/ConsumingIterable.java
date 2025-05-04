package com.mategka.dava.analyzer.extension;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConsumingIterable<T> implements Iterable<T> {

  Collection<T> collection;

  public static <T> ConsumingIterable<T> over(Collection<T> collection) {
    if (!CollectionsX.isMutable(collection)) {
      throw new IllegalArgumentException("Collection is not mutable");
    }
    return new ConsumingIterable<>(collection);
  }

  /**
   * Returns a new consuming iterator.
   * After this method is called, the underlying collection may not be modified through means other than this iterator.
   */
  @Override
  public @NotNull Iterator<T> iterator() {
    var baseIterator = collection.iterator();
    return new Iterator<>() {
      @Override
      public boolean hasNext() {
        return baseIterator.hasNext();
      }

      @Override
      public T next() {
        var value = baseIterator.next();
        baseIterator.remove();
        return value;
      }
    };
  }

}
