package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.extension.Pair;
import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.git.Hash;
import com.mategka.dava.analyzer.spoon.CtEqPath;
import com.mategka.dava.analyzer.struct.property.*;
import com.mategka.dava.analyzer.struct.property.index.PropertyIndexable;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;
import com.mategka.dava.analyzer.struct.property.value.Kind;
import com.mategka.dava.analyzer.struct.property.value.type.KnownType;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.NoSuchElementException;

@FieldDefaults(level = AccessLevel.PRIVATE)
public final class Symbol2 implements PropertyIndexable {

  public record Context(@NonNull SymbolKey key, @NonNull Hash commit) {}

  @Getter
  Option<Context> context = Option.None();

  final Multimap<PrdRole, SymbolKey> predecessors = HashMultimap.create(2, 2);

  @Getter
  final PropertyMap properties;

  public Symbol2() {
    properties = new PropertyMap();
  }

  public static Symbol2 withPropertyMap(@NonNull PropertyMap properties) {
    return new Symbol2(properties);
  }

  private Symbol2(@NonNull PropertyMap properties) {
    this.properties = properties;
  }

  public @NotNull String getName() throws NoSuchElementException {
    return getPropertyValue(SimpleNameProperty.class).getOrThrow();
  }

  public @NotNull Kind getKind() throws NoSuchElementException {
    return getPropertyValue(KindProperty.class).getOrThrow();
  }

  public List<Pair<PrdRole, SymbolKey>> getPredecessors() {
    return predecessors.entries().stream().map(Pair::fromEntry).toList();
  }

  public List<Pair<PrdRole, SymbolKey>> getPredecessors(PrdRole role) {
    return predecessors.get(role).stream().map(k -> Pair.of(role, k)).toList();
  }

  public void addPredecessor(PrdRole role, SymbolKey key) {
    predecessors.put(role, key);
  }

  public void clearPredecessors() {
    predecessors.clear();
  }

  public void putProperty(Property property) {
    properties.put(property);
  }

  public void setContext(@NonNull Context context) {
    this.context = Option.Some(context);
  }

  public @NotNull SymbolKey getKey() throws NoSuchElementException {
    return context.getOrThrow().key();
  }

  public @NotNull SymbolKey getParentKey() throws NoSuchElementException {
    return getPropertyValue(ParentProperty.class)
      .map(KnownType::getSymbolId)
      .map(symbolId -> new SymbolKey(symbolId, getKey().strandId()))
      .getOrThrow(() -> new NoSuchElementException("Symbol has no known parent (might it be the root package?)"));
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

  public boolean isRootPackage() {
    var name = getPropertyValue(SimpleNameProperty.class);
    var kind = getPropertyValue(KindProperty.class);
    var parent = getPropertyValue(ParentProperty.class);
    if (name.isNone() || kind.isNone() || parent.isSome()) {
      return false;
    }
    return SimpleNameProperty.ROOT_PACKAGE_NAME.equals(name.getOrThrow()) && Kind.PACKAGE.equals(kind.getOrThrow());
  }

  @SuppressWarnings({ "MethodDoesntCallSuperMethod" })
  @Override
  public Symbol2 clone() {
    return new Symbol2(properties.clone());
  }

  /*
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Symbol2 symbol)) return false;
    if (context.isNone() != symbol.context.isNone()) return false;
    if (context.isNone()) {
      return properties.equals(symbol.properties);
    }
    var thisContext = context.getOrThrow();
    var thatContext = symbol.context.getOrThrow();
    return Objects.equals(thisContext, thatContext) && Objects.equals(predecessors, symbol.predecessors);
  }

  @Override
  public int hashCode() {
    return context.fold(c -> Objects.hash(c, predecessors), properties::hashCode);
  }
  */

  @Override
  public String toString() {
    return getDisplayName();
  }

}
