package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.extension.Copyable;
import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.struct.Pair;
import com.mategka.dava.analyzer.spoon.path.CtEqPath;
import com.mategka.dava.analyzer.struct.property.*;
import com.mategka.dava.analyzer.struct.property.index.PropertyIndexable;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;
import com.mategka.dava.analyzer.struct.property.value.Kind;

import com.google.common.collect.HashMultimap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@FieldDefaults(level = AccessLevel.PRIVATE)
public final class Symbol implements PropertyIndexable, Copyable<Symbol>, Serializable {

  @Serial
  private static final long serialVersionUID = 3653794549169386469L;

  final HashMultimap<PrdRole, SymbolKey> predecessors = HashMultimap.create(2, 2);

  @Getter
  final PropertyMap properties;

  @Getter
  Option<Context> context = Option.None();

  public Symbol() {
    properties = new PropertyMap();
  }

  private Symbol(@NonNull PropertyMap properties) {
    this.properties = properties;
  }

  public static Symbol withPropertyMap(@NonNull PropertyMap properties) {
    return new Symbol(properties);
  }

  public void addPredecessor(PrdRole role, SymbolKey key) {
    predecessors.put(role, key);
  }

  public void clearPredecessors() {
    predecessors.clear();
  }

  @Override
  public @NotNull Symbol copy() {
    return clone();
  }

  public @NotNull Symbol copyWithContext() {
    var result = copy();
    result.context = context;
    return result;
  }

  public @NotNull String getDisplayName() {
    return getPropertyValue(SimpleNameProperty.class)
      .getOrCompute(() -> "(unnamed %s)".formatted(
        getPropertyValue(KindProperty.class)
          .map(Kind::toPseudoKeyword)
          .getOrElse("symbol")
      ));
  }

  public @NotNull SymbolKey getKey() throws NoSuchElementException {
    return context.getOrThrow().key();
  }

  public @NotNull Kind getKind() throws NoSuchElementException {
    return getPropertyValue(KindProperty.class).getOrThrow();
  }

  public @NotNull String getName() throws NoSuchElementException {
    return getPropertyValue(SimpleNameProperty.class).getOrThrow();
  }

  public @NotNull SymbolKey getParentKey() throws NoSuchElementException {
    return getPropertyValue(ParentProperty.class)
      .map(symbolId -> new SymbolKey(symbolId, getKey().strandId()))
      .getOrThrow(() -> new NoSuchElementException("Symbol has no known parent (might it be the root package?)"));
  }

  /**
   * @deprecated Use {@link #getSpoonPath()} instead.
   */
  @Deprecated
  public @NotNull CtEqPath getPath() throws NoSuchElementException {
    return getPropertyValue(CtPathProperty.class)
      .getOrThrow(() -> new NoSuchElementException("Symbol has no known path"));
  }

  public List<Pair<PrdRole, SymbolKey>> getPredecessors() {
    return predecessors.entries().stream().map(Pair::fromEntry).toList();
  }

  public List<Pair<PrdRole, SymbolKey>> getPredecessors(PrdRole role) {
    return predecessors.get(role).stream().map(k -> Pair.of(role, k)).toList();
  }

  public @NotNull String getSpoonPath() throws NoSuchElementException {
    return getPropertyValue(SpoonPathProperty.class)
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

  public int looseHashCode() {
    return context.fold(c -> Objects.hash(c, predecessors), properties::hashCode);
  }

  public boolean looselyEquals(Object o) {
    if (!(o instanceof Symbol symbol)) return false;
    if (context.isNone() != symbol.context.isNone()) return false;
    if (context.isNone()) {
      return properties.equals(symbol.properties);
    }
    var thisContext = context.getOrThrow();
    var thatContext = symbol.context.getOrThrow();
    return Objects.equals(thisContext, thatContext) && Objects.equals(predecessors, symbol.predecessors);
  }

  public void putProperty(Property property) {
    properties.put(property);
  }

  public void setContext(@NonNull Context context) {
    this.context = Option.Some(context);
  }

  @SuppressWarnings({ "MethodDoesntCallSuperMethod" })
  @Override
  public Symbol clone() {
    return new Symbol(properties.clone());
  }

  @Override
  public String toString() {
    return getPropertyValue(KindProperty.class).map(Kind::toPseudoKeyword).getOrThrow()
      + " " + getDisplayName()
      + context.map(c -> " [%s] ".formatted(c.key().symbolId())).getOrElse(" ")
      + properties;
  }

}
