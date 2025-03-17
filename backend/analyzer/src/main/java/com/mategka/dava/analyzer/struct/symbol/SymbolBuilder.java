package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.git.Hash;
import com.mategka.dava.analyzer.struct.property.Property;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class SymbolBuilder {

  private final List<SymbolKey> predecessors = new ArrayList<>();
  private final PropertyMap properties = new PropertyMap();
  private SymbolKey key;
  private Hash commit;

  SymbolBuilder() {
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

  public SymbolBuilder noPredecessors() {
    this.predecessors.clear();
    return this;
  }

  public SymbolBuilder predecessor(SymbolKey key) {
    predecessors.add(key);
    return this;
  }

  public SymbolBuilder predecessors(@NonNull List<SymbolKey> predecessors) {
    this.predecessors.addAll(predecessors);
    return this;
  }

  public SymbolBuilder properties(@NonNull PropertyMap properties) {
    this.properties.putAll(properties);
    return this;
  }

  public SymbolBuilder property(@NonNull Property property) {
    this.properties.put(property);
    return this;
  }

  public String toString() {
    return "Symbol.SymbolBuilder(key=" + this.key + ", commit=" + this.commit + ", predecessors="
      + this.predecessors + ", properties=" + this.properties + ")";
  }

}
