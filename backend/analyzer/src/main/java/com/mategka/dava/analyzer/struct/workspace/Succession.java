package com.mategka.dava.analyzer.struct.workspace;

import com.mategka.dava.analyzer.struct.symbol.Symbol;

import java.util.Collection;

public record Succession(MutableStrandWorkspace workspace, Collection<Symbol> successors) {

}
