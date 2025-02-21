package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.extension.MyStream;
import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.spoon.CtEqPath;
import com.mategka.dava.analyzer.struct.property.*;
import com.mategka.dava.analyzer.struct.property.index.PropertyIndexable;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;
import com.mategka.dava.analyzer.struct.property.value.Kind;
import com.mategka.dava.analyzer.struct.property.value.type.KnownType;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Builder(toBuilder = true)
public final class Symbol implements PropertyIndexable {

  public static final String ROOT_PACKAGE_NAME = "ROOT";

  @NonNull
  SymbolKey key;

  @NonNull
  String commitSha;

  @NonNull
  @Builder.Default
  List<Long> predecessors = new ArrayList<>();

  @NonNull
  @Builder.Default
  PropertyMap properties = new PropertyMap();

  public static boolean isRootPackage(Symbol symbol) {
    var name = symbol.getPropertyValue(SimpleNameProperty.class);
    var kind = symbol.getPropertyValue(KindProperty.class);
    var parent = symbol.getPropertyValue(ParentProperty.class);
    if (name.isNone() || kind.isNone() || parent.isSome()) {
      return false;
    }
    return ROOT_PACKAGE_NAME.equals(name.getOrThrow()) && Kind.PACKAGE.equals(kind.getOrThrow());
  }

  public @NotNull String getDisplayName() {
    return getPropertyValue(SimpleNameProperty.class)
      .getOrCompute(() -> "(unnamed %s)".formatted(
        getPropertyValue(KindProperty.class)
          .map(Kind::toPseudoKeyword)
          .getOrElse("symbol")
      ));
  }

  public @NotNull CtEqPath getPath() throws NoSuchElementException {
    return getPropertyValue(PathProperty.class)
      .getOrThrow(() -> new NoSuchElementException("Symbol has no known path"));
  }

  private long getParentId() throws NoSuchElementException {
    return getPropertyValue(ParentProperty.class)
      .map(KnownType::getSymbolId)
      .getOrThrow(() -> new NoSuchElementException("Symbol has no known parent (might it be the root package?)"));
  }

  public SymbolKey getParentKey() throws NoSuchElementException {
    return new SymbolKey(getParentId(), key.strandId());
  }

  public PropertyMap diff(@NotNull Collection<Property> newProperties) {
    return newProperties.stream()
      .filter(p -> Option.Some(p.getKey())
        .map(properties::get)
        .map(Property::value)
        .map(v -> !v.equals(p.value()))
        .getOrElse(false)
      )
      .collect(PropertyMap.collectProperties());
  }

  public Symbol withProperty(Property property) {
    return toBuilder().property(property).build();
  }

  public Symbol withUpdate(@NotNull SymbolUpdate update) {
    assertUpdatesApply(List.of(update));
    return toBuilder().update(update).build();
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
    var violation = MyStream.from(updates)
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

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Symbol symbol)) return false;
    return key.equals(symbol.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return "[%d] %s".formatted(key.symbolId(), getDisplayName());
  }

  public static class SymbolBuilder {

    public SymbolBuilder predecessor(long id) {
      if (!this.predecessors$set) {
        this.predecessors$set = true;
        this.predecessors$value = new ArrayList<>();
      }
      predecessors$value.add(id);
      return this;
    }

    public SymbolBuilder property(@NonNull Option<? extends Property> property) {
      return property.fold(this::property, () -> this);
    }

    public SymbolBuilder property(@NonNull Property property) {
      if (!this.properties$set) {
        this.properties$set = true;
        this.properties$value = new PropertyMap();
      }
      properties$value.put(property.getKey(), property);
      return this;
    }

    public SymbolBuilder update(@NonNull SymbolUpdate update) {
      if (!this.properties$set) {
        this.properties$set = true;
        this.properties$value = new PropertyMap();
      }
      properties$value.putAll(update.getProperties());
      return this;
    }

  }

}
