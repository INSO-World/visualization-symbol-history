package com.mategka.dava.analyzer.diff.symbol.pipeline;

import com.mategka.dava.analyzer.struct.symbol.Symbol;

import java.util.Set;

public record ExternalMappingSets(Set<Symbol> deletions, Set<Symbol> additions) {

}
