package com.mategka.dava.analyzer.diff.symbol;

import com.mategka.dava.analyzer.collections.Array;
import com.mategka.dava.analyzer.collections.ManyToManyMap;
import com.mategka.dava.analyzer.diff.file.FileMapping;
import com.mategka.dava.analyzer.diff.symbol.pipeline.*;
import com.mategka.dava.analyzer.diff.symbol.pipeline.struct.BaseSymbolSets;
import com.mategka.dava.analyzer.diff.symbol.pipeline.struct.ExternalMappingSets;
import com.mategka.dava.analyzer.diff.workspace.SymbolWorkspace;
import com.mategka.dava.analyzer.struct.symbol.Symbol;
import com.mategka.dava.analyzer.struct.symbol.SymbolCreationContext;
import com.mategka.dava.analyzer.struct.symbol.SymbolUpdate;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@UtilityClass
public class SymbolDiff {

  public SymbolMappingResult getMapping(SymbolWorkspace targetWorkspace, Array<SymbolWorkspace> parentWorkspaces,
                                        FileMapping fileMapping,
                                        SymbolCreationContext context) {
    boolean breakCommit = context.hasStrandChange();
    Array<ManyToManyMap<@NotNull Symbol, @NotNull Symbol, @Nullable Void>> symbolMaps = Array.fromSupplier(
      parentWorkspaces.length, ManyToManyMap::new);
    // 1. Map packages to each other
    PackageMapping.mapPackageSymbols(parentWorkspaces, symbolMaps, targetWorkspace);
    // 2. Map file symbols to each other
    IntraFileMapping.mapInnerSymbols(parentWorkspaces, fileMapping, symbolMaps, targetWorkspace, breakCommit);
    // 3. Identify (global) deletions and additions
    ExternalMappingSets externalMappings = ExternalMapping.computeExternalMappings(
      parentWorkspaces, targetWorkspace, symbolMaps);
    // 4. Choose suitable Context for all target symbols based on all sources, set predecessors, create prop updates
    List<SymbolUpdate> updates = MappingProcessing.processSymbolMappings(
      targetWorkspace, parentWorkspaces, context, externalMappings, symbolMaps);
    // 5. Identify succession symbols (needed for serialization), and copy additions and successions for a snapshot
    BaseSymbolSets baseSymbols = BaseSymbolExtraction.extractBaseSymbolSets(
      targetWorkspace, externalMappings, breakCommit);
    return new SymbolMappingResult(
      baseSymbols.additions(), externalMappings.deletions(), baseSymbols.successions(), updates);
  }

}
