package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.git.Hash;
import com.mategka.dava.analyzer.struct.property.ParentProperty;
import com.mategka.dava.analyzer.struct.property.index.PropertyIndexable;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;
import com.mategka.dava.analyzer.struct.property.value.type.KnownType;

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
  List<Long> predecessors;

  public Symbol(@NonNull SymbolKey key, @NonNull Hash commit, @NonNull List<Long> predecessors,
                @NonNull PropertyMap properties) {
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

  public Symbol succeed(long strandId) {
    return toBuilder().key(new SymbolKey(key.symbolId(), strandId)).build();
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

  public static class SymbolBuilder {

    private final List<Long> predecessors = new ArrayList<>();
    private final PropertyMap properties = new PropertyMap();
    private SymbolKey key;
    private Hash commit;

    private SymbolBuilder() {
    }

    public Symbol build() {
      return new Symbol(this.key, this.commit, this.predecessors, this.properties);
    }

    public SymbolBuilder commit(@NonNull Hash commit) {
      this.commit = commit;
      return this;
    }

    public SymbolBuilder key(@NonNull SymbolKey key) {
      this.key = key;
      return this;
    }

    public SymbolBuilder predecessor(long id) {
      predecessors.add(id);
      return this;
    }

    public SymbolBuilder predecessors(@NonNull List<Long> predecessors) {
      this.predecessors.addAll(predecessors);
      return this;
    }

    public SymbolBuilder properties(@NonNull PropertyMap properties) {
      this.properties.putAll(properties);
      return this;
    }

    public String toString() {
      return "Symbol.SymbolBuilder(key=" + this.key + ", commit=" + this.commit + ", predecessors="
        + this.predecessors + ", properties=" + this.properties + ")";
    }

  }

}
