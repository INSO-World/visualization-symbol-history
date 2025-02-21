package com.mategka.dava.analyzer.struct.property;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

public sealed interface SetProperty<T> extends TypedProperty<Set<? extends T>>
  permits ModifiersProperty {

  @NotNull Set<? extends T> value();

  default int size() {
    return value().size();
  }

  default boolean isEmpty() {
    return value().isEmpty();
  }

  default boolean contains(T o) {
    return value().contains(o);
  }

  default boolean containsAll(@NotNull Collection<? extends T> c) {
    return value().containsAll(c);
  }

}
