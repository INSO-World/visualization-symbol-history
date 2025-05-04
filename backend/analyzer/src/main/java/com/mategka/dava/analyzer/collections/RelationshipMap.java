package com.mategka.dava.analyzer.collections;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.jetbrains.annotations.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public interface RelationshipMap<S, T, M> {

  @NotNull Collection<Mapping<S, T, M>> getBySource(@Nullable S source);

  @NotNull Collection<Mapping<S, T, M>> getByTarget(@Nullable T target);

  int getSourceCount(@Nullable T target);

  int getTargetCount(@Nullable S source);

  default @NotNull Set<S> getUnmappedSources(@NotNull Collection<? extends S> sources) {
    Set<S> sourcesSet = new HashSet<>(sources);
    sourcesSet.removeAll(sources());
    return sourcesSet;
  }

  default @NotNull Set<T> getUnmappedTargets(@NotNull Collection<? extends T> targets) {
    Set<T> targetsSet = new HashSet<>(targets);
    targetsSet.removeAll(targets());
    return targetsSet;
  }

  default boolean hasSource(@Nullable S source) {
    return sources().contains(source);
  }

  default boolean hasTarget(@Nullable T target) {
    return targets().contains(target);
  }

  @UnmodifiableView
  @NotNull Collection<Mapping<S, T, M>> mappings();

  @CanIgnoreReturnValue
  @UnknownNullability
  Mapping<S, T, M> put(@Nullable S source, @Nullable T target, @Nullable M metadata);

  @CanIgnoreReturnValue
  default boolean remove(@NotNull Mapping<S, T, M> mapping) {
    return remove(mapping.source(), mapping.target(), mapping.metadata()) != null;
  }

  @CanIgnoreReturnValue
  @UnknownNullability
  Mapping<S, T, M> remove(@Nullable S source, @Nullable T target);

  @CanIgnoreReturnValue
  @UnknownNullability
  Mapping<S, T, M> remove(@Nullable S source, @Nullable T target, @Nullable M metadata);

  @CanIgnoreReturnValue
  boolean removeBySource(@Nullable S source);

  @CanIgnoreReturnValue
  boolean removeByTarget(@Nullable T target);

  @UnmodifiableView
  @NotNull Set<S> sources();

  @UnmodifiableView
  @NotNull Set<T> targets();

  @NotNull ManyToManyMap<S, T, M> toManyToManyMap();

}
