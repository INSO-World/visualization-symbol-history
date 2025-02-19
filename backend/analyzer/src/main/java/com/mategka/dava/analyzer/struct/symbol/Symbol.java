package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.extension.MyStream;
import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.spoon.CtEqPath;
import com.mategka.dava.analyzer.struct.property.*;
import com.mategka.dava.analyzer.struct.property.index.PropertyIndexable;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;
import com.mategka.dava.analyzer.struct.property.value.Kind;
import com.mategka.dava.analyzer.struct.property.value.KnownType;

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
  long id;
  long strandId;
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

  public @NotNull Key getKey() {
    return new Key(id, strandId);
  }

  private long getParentId() throws NoSuchElementException {
    return getPropertyValue(ParentProperty.class)
      .map(KnownType::getId)
      .getOrThrow(() -> new NoSuchElementException("Symbol has no known parent (might it be the root package?)"));
  }

  public Key getParentKey() throws NoSuchElementException {
    return new Key(getParentId(), strandId);
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
    if (!update.appliesTo(this)) {
      throw new IllegalArgumentException(
        "Cannot apply update for symbol %d@%d to symbol %d@%d".formatted(
          update.getId(), update.getStrandId(), id, strandId)
      );
    }
    return toBuilder().update(update).build();
  }

  public Symbol withUpdates(@NotNull Collection<? extends SymbolUpdate> updates) {
    var violation = MyStream.from(updates).filter(u -> !u.appliesTo(this)).findFirstAsOption();
    if (violation.isSome()) {
      throw new IllegalArgumentException(
        "Cannot apply update for symbol %d@%d to symbol %d@%d".formatted(
          violation.getOrThrow().getId(), violation.getOrThrow().getStrandId(), id, strandId)
      );
    }
    var updatedProperties = updates.stream()
      .map(SymbolUpdate::getProperties)
      .map(Map::entrySet)
      .flatMap(Collection::stream)
      .collect(PropertyMap.collectEntries());
    return toBuilder().properties(updatedProperties).build();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Symbol symbol)) return false;
    return id == symbol.id && strandId == symbol.strandId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, strandId);
  }

  @Override
  public String toString() {
    return "[%d] %s".formatted(id, getDisplayName());
  }

  public record Key(long symbolId, long strandId) {

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
