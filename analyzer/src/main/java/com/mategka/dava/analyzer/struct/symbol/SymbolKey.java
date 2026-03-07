package com.mategka.dava.analyzer.struct.symbol;

import java.io.Serial;
import java.io.Serializable;

public record SymbolKey(long symbolId, long strandId) implements Serializable {

  @Serial
  private static final long serialVersionUID = -4863728521777941822L;

  @Override
  public String toString() {
    return "%s@%s".formatted(symbolId, strandId);
  }

}
