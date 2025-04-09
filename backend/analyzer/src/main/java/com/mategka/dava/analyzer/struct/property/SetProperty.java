package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.extension.Covariant;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

public sealed interface SetProperty<T> extends TypedProperty<Set<? extends T>>
  permits ModifiersProperty {

  default boolean contains(T o) {
    return value().contains(o);
  }

  default boolean containsAll(@NotNull Collection<? extends T> c) {
    return value().containsAll(Covariant.collection(c));
  }

  default boolean isEmpty() {
    return value().isEmpty();
  }

  default int size() {
    return value().size();
  }

  @NotNull Set<T> value();

}
