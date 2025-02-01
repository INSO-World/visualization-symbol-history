package com.mategka.dava.analyzer.struct;

import com.mategka.dava.analyzer.struct.symbol.Symbol;

import java.util.concurrent.atomic.AtomicLong;

public record SymbolCreationContext(AtomicLong symbolIdCounter, long strandId, String commitSha) {

  public Symbol.SymbolBuilder symbolBuilder() {
    return Symbol.builder()
      .id(symbolIdCounter.getAndIncrement())
      .strandId(strandId)
      .commitSha(commitSha);
  }

}
