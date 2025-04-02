package com.mategka.dava.analyzer.collections;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ManyToOneMap<S, T, M> implements RelationshipMap<S, T, M> {

  Map<S, Mapping<S, T, M>> sourceMap = new HashMap<>();
  Table<T, S, Mapping<S, T, M>> targetTable = HashBasedTable.create();

  @Override
  public @NotNull Collection<Mapping<S, T, M>> mappings() {
    return Collections.unmodifiableCollection(sourceMap.values());
  }

  @Override
  public @NotNull List<Mapping<S, T, M>> getBySource(S source) {
    return List.of(sourceMap.get(source));
  }

  @Override
  public @NotNull List<Mapping<S, T, M>> getByTarget(T target) {
    return targetTable.row(target).values().stream().toList();
  }

  @Override
  public int getSourceCount(T target) {
    return targetTable.row(target).size();
  }

  @Override
  public int getTargetCount(@Nullable S source) {
    return hasSource(source) ? 1 : 0;
  }

  @Override
  public @NotNull Set<T> targets() {
    return Collections.unmodifiableSet(targetTable.rowKeySet());
  }

  @Override
  public @NotNull Set<S> sources() {
    return Collections.unmodifiableSet(targetTable.columnKeySet());
  }

  @Override
  @CanIgnoreReturnValue
  public Mapping<S, T, M> put(S source, T target, M metadata) {
    var oldMappings = getBySource(source);
    if (!oldMappings.isEmpty()) {
      removeBySource(source);
    }
    var newMapping = new Mapping<>(source, target, metadata);
    sourceMap.put(source, newMapping);
    targetTable.put(target, source, newMapping);
    return oldMappings.isEmpty() ? null : oldMappings.getFirst();
  }

  @Override
  public @UnknownNullability Mapping<S, T, M> remove(@Nullable S source, @Nullable T target) {
    var oldMapping = sourceMap.get(source);
    if (oldMapping != null && oldMapping.target().equals(target)) {
      sourceMap.remove(source);
      targetTable.remove(target, source);
      return oldMapping;
    }
    return null;
  }

  @Override
  @CanIgnoreReturnValue
  @UnknownNullability
  public Mapping<S, T, M> remove(@Nullable S source, @Nullable T target, @Nullable M metadata) {
    var oldMapping = sourceMap.get(source);
    if (oldMapping != null && oldMapping.target().equals(target) && oldMapping.metadata().equals(metadata)) {
      sourceMap.remove(source);
      targetTable.remove(target, source);
      return oldMapping;
    }
    return null;
  }

  @Override
  public boolean removeBySource(@Nullable S source) {
    var mapping = sourceMap.remove(source);
    if (mapping == null) {
      return false;
    }
    targetTable.remove(mapping.target(), source);
    return true;
  }

  @Override
  public boolean removeByTarget(@Nullable T target) {
    var hadTarget = hasTarget(target);
    targetTable.row(target).clear();
    return hadTarget;
  }

  @Override
  public @NotNull ManyToManyMap<S, T, M> toManyToManyMap() {
    return new ManyToManyMap<>(targetTable);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ManyToOneMap<?, ?, ?> that = (ManyToOneMap<?, ?, ?>) o;
    return Objects.equals(mappings(), that.mappings());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(mappings());
  }

}
