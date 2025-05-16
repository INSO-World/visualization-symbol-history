package com.mategka.dava.analyzer.diff.symbol.pipeline;

import com.mategka.dava.analyzer.diff.symbol.pipeline.struct.BaseSymbolSets;
import com.mategka.dava.analyzer.diff.symbol.pipeline.struct.ExternalMappingSets;
import com.mategka.dava.analyzer.diff.workspace.SymbolWorkspace;
import com.mategka.dava.analyzer.extension.SetsX;
import com.mategka.dava.analyzer.struct.symbol.Symbol;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

public class BaseSymbolExtraction {

  public static @NotNull BaseSymbolSets extractBaseSymbolSets(SymbolWorkspace targetWorkspace,
                                                              @NotNull ExternalMappingSets externalMappings,
                                                              boolean breakCommit) {
    Set<Symbol> successionsSet = breakCommit
      ? targetWorkspace.getSuccessions(externalMappings.additions())
      : Collections.emptySet();
    // Copy additions and successions (since they will be mutated within a strand to save memory, losing its state)
    Set<Symbol> additions = SetsX.copy(externalMappings.additions(), Symbol::copyWithContext);
    Set<Symbol> successions = breakCommit ? SetsX.copy(successionsSet, Symbol::copyWithContext) : successionsSet;
    return new BaseSymbolSets(additions, successions);
  }

}
