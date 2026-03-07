package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.git.Hash;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;

public record SymbolCreationContext(AtomicLong symbolIdCounter, long strandId, Hash commit, boolean hasStrandChange) {

  public @NotNull Context generateContext() {
    return new Context(generateKey(), commit);
  }

  @Contract(" -> new")
  public @NotNull SymbolKey generateKey() {
    return new SymbolKey(symbolIdCounter.getAndIncrement(), strandId);
  }

}
