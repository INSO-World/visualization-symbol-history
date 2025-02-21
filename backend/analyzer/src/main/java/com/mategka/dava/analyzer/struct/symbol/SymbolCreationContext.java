package com.mategka.dava.analyzer.struct.symbol;

import java.util.concurrent.atomic.AtomicLong;

public record SymbolCreationContext(AtomicLong symbolIdCounter, long strandId, String commitSha) {

  public Symbol.SymbolBuilder symbolBuilder() {
    return Symbol.builder()
      .key(new SymbolKey(symbolIdCounter.getAndIncrement(), strandId))
      .commitSha(commitSha);
  }

}
