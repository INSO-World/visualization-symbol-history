package com.mategka.dava.analyzer.diff.symbol.pipeline.struct;

import com.mategka.dava.analyzer.struct.symbol.Symbol;

import java.util.Set;

public record BaseSymbolSets(Set<Symbol> additions, Set<Symbol> successions) {

}
