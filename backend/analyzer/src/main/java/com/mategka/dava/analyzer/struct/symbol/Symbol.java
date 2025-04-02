package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.git.Hash;
import com.mategka.dava.analyzer.struct.property.ParentProperty;
import com.mategka.dava.analyzer.struct.property.Property;
import com.mategka.dava.analyzer.struct.property.index.PropertyIndexable;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;
import com.mategka.dava.analyzer.struct.property.value.type.KnownType;

import com.google.common.collect.Multimap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class Symbol extends BareSymbol implements PropertyIndexable {

  @NonNull
  SymbolKey key;

  @NonNull
  Hash commit;

  @NonNull
  Multimap<PrdRole, SymbolKey> predecessors;

  public Symbol(@NonNull SymbolKey key, @NonNull Hash commit,
                @NonNull Multimap<PrdRole, SymbolKey> predecessors, @NonNull PropertyMap properties) {
    super(properties);
    this.key = key;
    this.commit = commit;
    this.predecessors = predecessors;
  }

  public static SymbolBuilder builder() {
    return new SymbolBuilder();
  }

  public long getId() {
    return key.symbolId();
  }

  public long getParentId() throws NoSuchElementException {
    return getPropertyValue(ParentProperty.class)
      .map(KnownType::getSymbolId)
      .getOrThrow(() -> new NoSuchElementException("Symbol has no known parent (might it be the root package?)"));
  }

  public SymbolKey getParentKey() throws NoSuchElementException {
    return new SymbolKey(getParentId(), key.strandId());
  }

  public long getStrandId() {
    return key.strandId();
  }

  public Symbol succeedOneToOne(long strandId) {
    var successorKey = new SymbolKey(key.symbolId(), strandId);
    return toBuilder()
      .noPredecessors().predecessor(PrdRole.DIRECT, key)
      .key(successorKey)
      .build();
  }

  public SymbolBuilder toBuilder() {
    return new SymbolBuilder().key(this.key)
      .commit(this.commit)
      .predecessors(this.predecessors)
      .properties(this.properties);
  }

  public Symbol withUpdate(@NotNull SymbolUpdate update) {
    assertUpdatesApply(List.of(update));
    return toBuilder().properties(update.getProperties()).build();
  }

  public Symbol withUpdates(@NotNull Collection<? extends SymbolUpdate> updates) {
    assertUpdatesApply(updates);
    var updatedProperties = updates.stream()
      .map(SymbolUpdate::getProperties)
      .map(Map::entrySet)
      .flatMap(Collection::stream)
      .collect(PropertyMap.collectEntries());
    return toBuilder().properties(updatedProperties).build();
  }

  @Override
  public Symbol withProperty(Property property) {
    return toBuilder().property(property).build();
  }

  private void assertUpdatesApply(@NotNull Collection<? extends SymbolUpdate> updates) {
    var violation = AnStream.from(updates)
      .filter(u -> !u.appliesTo(this))
      .findFirstAsOption();
    if (violation.isSome()) {
      var key = violation.getOrThrow().getKey();
      throw new IllegalArgumentException(
        "Cannot apply update for symbol %d@%d to symbol %d@%d".formatted(
          key.symbolId(), key.strandId(), this.key.symbolId(), this.key.strandId())
      );
    }
  }

  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @Override
  public Symbol clone() {
    return toBuilder().build();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Symbol that)) return false;
    if (!super.equals(o)) return false;
    return Objects.equals(key, that.key) && Objects.equals(commit, that.commit)
      && Objects.equals(predecessors, that.predecessors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), key, commit, predecessors);
  }

  @Override
  public String toString() {
    return "[%d] %s".formatted(key.symbolId(), getDisplayName());
  }

}
