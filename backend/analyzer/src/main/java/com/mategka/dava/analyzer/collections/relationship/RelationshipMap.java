package com.mategka.dava.analyzer.collections.relationship;

import com.mategka.dava.analyzer.extension.stream.AnStream;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.*;

import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RelationshipMap<S, T, M> {

  Map<T, Mapping<S, @NotNull T, M>> additions = new HashMap<>();
  Map<S, Mapping<@NotNull S, T, M>> deletions = new HashMap<>();
  Table<T, S, Mapping<@NotNull S, @NotNull T, M>> mappings = HashBasedTable.create();

  @CanIgnoreReturnValue
  public boolean clearAdditions() {
    var changed = !additions.isEmpty();
    additions.clear();
    return changed;
  }

  @CanIgnoreReturnValue
  public boolean clearDeletions() {
    var changed = !deletions.isEmpty();
    deletions.clear();
    return changed;
  }

  @Unmodifiable
  public @NotNull Collection<@NotNull Mapping<S, T, M>> getBySource(@Nullable S source) {
    if (deletions.containsKey(source)) {
      return Collections.singleton(deletions.get(source));
    }
    return Collections.unmodifiableCollection(mappings.column(source).values());
  }

  @Unmodifiable
  public @NotNull Collection<Mapping<S, T, M>> getByTarget(@Nullable T target) {
    if (additions.containsKey(target)) {
      return Collections.singleton(additions.get(target));
    }
    return Collections.unmodifiableCollection(mappings.row(target).values());
  }

  public int getSourceCount(@Nullable T target) {
    return mappings.row(target).size();
  }

  public int getTargetCount(@Nullable S source) {
    return mappings.column(source).size();
  }

  public boolean hasSource(@Nullable S source) {
    return sources().contains(source);
  }

  public boolean hasTarget(@Nullable T target) {
    return targets().contains(target);
  }

  @Unmodifiable
  public @NotNull Collection<Mapping<S, T, M>> mappings() {
    return AnStream.sequence(additions.values(), deletions.values(), mappings.values())
      .flatMap(Collection::stream)
      .toList();
  }

  @CanIgnoreReturnValue
  public boolean put(@NotNull Mapping<S, T, M> mapping) {
    if (mapping.isAddition()) {
      return removeByTarget(mapping.target()) | additions.put(mapping.target(), mapping) != null;
    }
    if (mapping.isDeletion()) {
      return removeBySource(mapping.source()) | deletions.put(mapping.source(), mapping) != null;
    }
    return removeExternals(mapping.source(), mapping.target())
      | mappings.put(mapping.target(), mapping.source(), mapping) != null;
  }

  @CanIgnoreReturnValue
  public boolean put(@Nullable S source, @Nullable T target, @Nullable M metadata) {
    return put(Mapping.create(source, target, metadata));
  }

  @CanIgnoreReturnValue
  public boolean putAddition(@NotNull T target, @Nullable M metadata) {
    return put(Mapping.createAddition(target, metadata));
  }

  @CanIgnoreReturnValue
  public boolean putAddition(@NotNull T target) {
    return putAddition(target, null);
  }

  @CanIgnoreReturnValue
  public boolean putDeletion(@NotNull S source, @Nullable M metadata) {
    return put(Mapping.createDeletion(source, metadata));
  }

  @CanIgnoreReturnValue
  public boolean putDeletion(@NotNull S source) {
    return putDeletion(source, null);
  }

  @CanIgnoreReturnValue
  public boolean remove(@NotNull Mapping<S, T, M> mapping) {
    if (mapping.isAddition()) {
      return additions.remove(mapping.target(), mapping);
    }
    if (mapping.isDeletion()) {
      return deletions.remove(mapping.source(), mapping);
    }
    var associatedMapping = mappings.get(mapping.target(), mapping.source());
    if (associatedMapping == null) {
      return false;
    }
    if (Objects.equals(associatedMapping.metadata(), mapping.metadata())) {
      mappings.remove(mapping.target(), mapping.source());
      return true;
    }
    return false;
  }

  @CanIgnoreReturnValue
  @UnknownNullability
  public Mapping<S, T, M> remove(@Nullable S source, @Nullable T target) {
    if (source == null) {
      return additions.remove(target);
    }
    if (target == null) {
      return deletions.remove(source);
    }
    return mappings.remove(target, source);
  }

  @CanIgnoreReturnValue
  public boolean remove(@Nullable S source, @Nullable T target, @Nullable M metadata) {
    return remove(Mapping.create(source, target, metadata));
  }

  @CanIgnoreReturnValue
  public boolean removeBySource(@NotNull S source) {
    var changed = hasSource(source);
    mappings.column(source).clear();
    return changed;
  }

  @CanIgnoreReturnValue
  public boolean removeByTarget(@NotNull T target) {
    var changed = hasTarget(target);
    mappings.row(target).clear();
    return changed;
  }

  @UnmodifiableView
  public @NotNull Set<S> sources() {
    return Collections.unmodifiableSet(mappings.columnKeySet());
  }

  @UnmodifiableView
  public @NotNull Set<T> targets() {
    return Collections.unmodifiableSet(mappings.rowKeySet());
  }

  private boolean removeExternals(@NotNull S source, @NotNull T target) {
    return additions.remove(target) != null || deletions.remove(source) != null;
  }

}
