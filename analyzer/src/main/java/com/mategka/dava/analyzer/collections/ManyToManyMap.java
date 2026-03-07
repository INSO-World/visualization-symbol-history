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
public class ManyToManyMap<S, T, M> implements RelationshipMap<S, T, M> {

  Table<T, S, Mapping<S, T, M>> table = HashBasedTable.create();

  public ManyToManyMap() {
  }

  ManyToManyMap(Table<T, S, Mapping<S, T, M>> table) {
    this.table.putAll(table);
  }

  @Override
  public @NotNull List<Mapping<S, T, M>> getBySource(S source) {
    return table.column(source).values().stream().toList();
  }

  @Override
  public @NotNull List<Mapping<S, T, M>> getByTarget(T target) {
    return table.row(target).values().stream().toList();
  }

  @Override
  public int getSourceCount(T target) {
    return table.row(target).size();
  }

  @Override
  public int getTargetCount(S source) {
    return table.column(source).size();
  }

  @Override
  public @NotNull Collection<Mapping<S, T, M>> mappings() {
    return Collections.unmodifiableCollection(table.values());
  }

  @Override
  @CanIgnoreReturnValue
  public Mapping<S, T, M> put(S source, T target, M metadata) {
    var oldMapping = remove(source, target);
    var newMapping = new Mapping<>(source, target, metadata);
    table.put(target, source, newMapping);
    return oldMapping;
  }

  @Override
  @CanIgnoreReturnValue
  public Mapping<S, T, M> remove(S source, T target) {
    return table.remove(target, source);
  }

  @Override
  public @UnknownNullability Mapping<S, T, M> remove(@Nullable S source, @Nullable T target, @Nullable M metadata) {
    var mapping = table.get(target, source);
    if (mapping == null) {
      return null;
    }
    return table.remove(target, source);
  }

  @Override
  @CanIgnoreReturnValue
  public boolean removeBySource(S source) {
    var hadSource = hasSource(source);
    table.column(source).clear();
    return hadSource;
  }

  @Override
  @CanIgnoreReturnValue
  public boolean removeByTarget(T target) {
    var hadTarget = hasTarget(target);
    table.row(target).clear();
    return hadTarget;
  }

  @Override
  public @NotNull Set<S> sources() {
    return Collections.unmodifiableSet(table.columnKeySet());
  }

  @Override
  public @NotNull Set<T> targets() {
    return Collections.unmodifiableSet(table.rowKeySet());
  }

  @Override
  public @NotNull ManyToManyMap<S, T, M> toManyToManyMap() {
    return new ManyToManyMap<>(HashBasedTable.create(table));
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ManyToManyMap<?, ?, ?> that = (ManyToManyMap<?, ?, ?>) o;
    return Objects.equals(table, that.table);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(table);
  }

}
