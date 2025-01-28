package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.struct.SymbolUpdate;
import com.mategka.dava.analyzer.struct.property.*;
import com.mategka.dava.analyzer.struct.property.index.PropertyIndexable;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import spoon.reflect.path.CtPath;

import java.util.*;

@Value
@Builder(toBuilder = true)
public class Symbol implements PropertyIndexable {

  public static final String ROOT_PACKAGE_NAME = "ROOT";

  long id;

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
    if (name.isEmpty() || kind.isEmpty() || parent.isPresent()) {
      return false;
    }
    return ROOT_PACKAGE_NAME.equals(name.get()) && KindProperty.Value.PACKAGE.equals(kind.get());
  }

  public static Symbol squash(Symbol base, Symbol current) {
    return current.toBuilder().id(base.id).build();
  }

  public @NotNull String getDisplayName() {
    return getPropertyValue(SimpleNameProperty.class)
      .orElseGet(() -> "(unnamed %s)".formatted(
        getPropertyValue(KindProperty.class)
          .map(KindProperty.Value::toPseudoKeyword)
          .orElse("symbol")
      ));
  }

  public @NotNull CtPath getPath() throws NoSuchElementException {
    return getPropertyValue(PathProperty.class)
      .orElseThrow(() -> new NoSuchElementException("Symbol has no known path"));
  }

  public long getParentId() throws NoSuchElementException {
    return getPropertyValue(ParentProperty.class)
      .map(TypeValue.KnownType::getId)
      .orElseThrow(() -> new NoSuchElementException("Symbol has no known parent (might it be the root package?)"));
  }

  public PropertyMap diff(@NotNull Collection<Property> newProperties) {
    return newProperties.stream()
      .filter(p -> Optional.of(p.getKey())
        .map(properties::get)
        .map(Property::value)
        .map(v -> !v.equals(p.value()))
        .orElse(false)
      )
      .collect(PropertyMap.collectProperties());
  }

  public Symbol withUpdate(@NotNull SymbolUpdate update) {
    if (!Objects.equals(id, update.getId())) {
      throw new IllegalArgumentException(
        "Cannot apply update for symbol %d to symbol %d".formatted(update.getId(), id)
      );
    }
    return toBuilder().update(update).build();
  }

  public Symbol withUpdates(@NotNull Collection<? extends SymbolUpdate> updates) {
    var violation = updates.stream().map(SymbolUpdate::getId).filter(i -> i != id).findFirst();
    if (violation.isPresent()) {
      throw new IllegalArgumentException(
        "Cannot apply update for symbol %d to symbol %d".formatted(violation.get(), id)
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
  public String toString() {
    return "[%d] %s".formatted(id, getDisplayName());
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
