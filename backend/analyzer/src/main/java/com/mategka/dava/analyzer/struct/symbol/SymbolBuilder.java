package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.git.Hash;
import com.mategka.dava.analyzer.struct.property.Property;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.NonNull;

public final class SymbolBuilder {

  private final Multimap<PrdRole, SymbolKey> predecessors = HashMultimap.create(PrdRole.SIZE, 2);
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

  public SymbolBuilder predecessor(PrdRole role, SymbolKey key) {
    predecessors.put(role, key);
    return this;
  }

  public SymbolBuilder predecessors(@NonNull Multimap<PrdRole, SymbolKey> predecessors) {
    this.predecessors.putAll(predecessors);
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
