package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.spoon.CtEqPath;
import com.mategka.dava.analyzer.struct.property.*;
import com.mategka.dava.analyzer.struct.property.index.PropertyIndexable;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;
import com.mategka.dava.analyzer.struct.property.value.Kind;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;

@Getter
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public sealed class BareSymbol implements PropertyIndexable permits Symbol {

  PropertyMap properties;

  public BareSymbol() {
    this.properties = new PropertyMap();
  }

  public BareSymbol(@NonNull PropertyMap properties) {
    this.properties = properties;
  }

  public @NotNull Symbol asReplacementFor(Symbol symbol, Symbol newParentSymbol) {
    return withProperty(ParentProperty.fromSymbol(newParentSymbol))
      .toSymbolBuilder()
      .key(symbol.getKey())
      .commit(symbol.getCommit())
      .build();
  }

  public @NotNull Symbol complete(SymbolCreationContext context) {
    return toSymbolBuilder()
      .key(new SymbolKey(context.symbolIdCounter().getAndIncrement(), context.strandId()))
      .commit(context.commit())
      .build();
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

  public @NotNull Symbol.SymbolBuilder toSymbolBuilder() {
    return Symbol.builder().properties(properties);
  }

  public BareSymbol withProperty(Property property) {
    return new BareSymbol(properties.with(property));
  }

  @SuppressWarnings({ "MethodDoesntCallSuperMethod" })
  @Override
  public BareSymbol clone() {
    return new BareSymbol(properties.clone());
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof BareSymbol symbol)) return false;
    return properties.equals(symbol.properties);
  }

  @Override
  public int hashCode() {
    return properties.hashCode();
  }

  @Override
  public String toString() {
    return getDisplayName();
  }

}
