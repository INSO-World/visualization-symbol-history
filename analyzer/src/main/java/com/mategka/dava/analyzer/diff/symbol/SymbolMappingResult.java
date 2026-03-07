package com.mategka.dava.analyzer.diff.symbol;

import com.mategka.dava.analyzer.struct.symbol.Symbol;
import com.mategka.dava.analyzer.struct.symbol.SymbolUpdate;

import java.util.Collection;
import java.util.Set;

public record SymbolMappingResult(
  Set<Symbol> additions,
  Set<Symbol> deletions,
  Set<Symbol> successions,
  Collection<SymbolUpdate> updates
) {

}
