package com.mategka.dava.analyzer.extension;

import com.google.common.collect.Comparators;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.SequencedCollection;
import java.util.function.Function;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Wrapped<T extends Wrapped<T>> implements Comparable<T> {

  @Getter
  T object;

  SequencedCollection<Function<T, Object>> componentGetters;

  @Override
  public final int compareTo(@NotNull T o) {
    return wrappedCompareTo(o);
  }

  protected final AnStream<Object> components() {
    return AnStream.from(componentGetters).map(getter -> getter.apply(object));
  }

  protected int wrappedCompareTo(T o) {
    return Comparators.lexicographical(ComparatorsX.nullsFirstComparator())
      .compare(components().iterable(), o.components().iterable());
  }

  protected boolean wrappedEqual(T o) {
    return componentGetters.stream()
      .map(getter -> Pair.of(getter.apply(object), getter.apply(o)))
      .allMatch(p -> Objects.equals(p.left(), p.right()));
  }

  protected int wrappedHashCode() {
    return Objects.hash(componentGetters.stream().map(getter -> getter.apply(object)).toArray());
  }

  @Override
  public final boolean equals(Object o) {
    if (getClass().isAssignableFrom(o.getClass())) return false;
    //noinspection unchecked
    return wrappedEqual((T) o);
  }

  @Override
  public final int hashCode() {
    return wrappedHashCode();
  }

}
