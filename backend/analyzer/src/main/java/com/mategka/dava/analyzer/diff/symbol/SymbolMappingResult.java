package com.mategka.dava.analyzer.diff.symbol;

import com.mategka.dava.analyzer.struct.symbol.Symbol2;
import com.mategka.dava.analyzer.struct.symbol.SymbolUpdate2;

import java.util.Collection;
import java.util.Set;

public record SymbolMappingResult(SymbolWorkspace workspace, Set<Symbol2> additions, Set<Symbol2> deletions, Collection<SymbolUpdate2> updates) {

}
