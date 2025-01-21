package com.mategka.dava.analyzer.struct.property;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public sealed interface SetProperty<T> extends Property, Set<T> permits EnumSetProperty {

  @NotNull Set<T> value();

  @Override
  default int size() {
    return value().size();
  }

  @Override
  default boolean isEmpty() {
    return value().isEmpty();
  }

  @Override
  default boolean contains(Object o) {
    return value().contains(o);
  }

  @Override
  default @NotNull Iterator<T> iterator() {
    return value().iterator();
  }

  @Override
  default @NotNull Object @NotNull [] toArray() {
    return value().toArray();
  }

  @Override
  default @NotNull <T1> T1 @NotNull [] toArray(@NotNull T1 @NotNull [] a) {
    return value().toArray(a);
  }

  @Override
  default boolean add(T t) {
    throw new UnsupportedOperationException("Properties are read-only");
  }

  @Override
  default boolean remove(Object o) {
    throw new UnsupportedOperationException("Properties are read-only");
  }

  @Override
  default boolean containsAll(@NotNull Collection<?> c) {
    return value().containsAll(c);
  }

  @Override
  default boolean addAll(@NotNull Collection<? extends T> c) {
    throw new UnsupportedOperationException("Properties are read-only");
  }

  @Override
  default boolean retainAll(@NotNull Collection<?> c) {
    throw new UnsupportedOperationException("Properties are read-only");
  }

  @Override
  default boolean removeAll(@NotNull Collection<?> c) {
    throw new UnsupportedOperationException("Properties are read-only");
  }

  @Override
  default void clear() {
    throw new UnsupportedOperationException("Properties are read-only");
  }

}
