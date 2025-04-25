package com.mategka.dava.analyzer.diff.symbol.pipeline;

import com.mategka.dava.analyzer.collections.Array;
import com.mategka.dava.analyzer.collections.CountingMap;
import com.mategka.dava.analyzer.collections.ManyToManyMap;
import com.mategka.dava.analyzer.diff.workspace.SymbolWorkspace;
import com.mategka.dava.analyzer.extension.ListsX;
import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.extension.struct.TreeNode;
import com.mategka.dava.analyzer.struct.symbol.Symbol;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@UtilityClass
public class ExternalMapping {

  public @NotNull ExternalMappingSets computeExternalMappings(Array<SymbolWorkspace> parentWorkspaces,
                                                              SymbolWorkspace targetWorkspace,
                                                              Array<ManyToManyMap<@NotNull Symbol, @NotNull Symbol, @Nullable Void>> symbolMaps) {
    var targetSymbols = ListsX.map(targetWorkspace.getLocatedSymbols().values(), TreeNode::value);
    var additionCounter = new CountingMap<Symbol>();
    Set<Symbol> deletions = computeDeletions(parentWorkspaces, symbolMaps, targetSymbols, additionCounter);
    Set<Symbol> additions = computeAdditions(parentWorkspaces, targetSymbols, additionCounter);
    return new ExternalMappingSets(deletions, additions);
  }

  private @NotNull Set<Symbol> computeDeletions(Array<SymbolWorkspace> parentWorkspaces,
                                                Array<ManyToManyMap<@NotNull Symbol, @NotNull Symbol, @Nullable Void>> symbolMaps,
                                                List<Symbol> targetSymbols,
                                                CountingMap<Symbol> additionCounter) {
    Set<Symbol> deletions = new HashSet<>();
    for (var parentWorkspaceTuple : parentWorkspaces.withIndex()) {
      var parentIndex = parentWorkspaceTuple.left();
      var parentWorkspace = parentWorkspaceTuple.right();
      var symbolMap = symbolMaps.get(parentIndex);
      deletions.addAll(symbolMap.getUnmappedSources(parentWorkspace.getAllSymbols()));
      for (var unmappedTarget : symbolMap.getUnmappedTargets(targetSymbols)) {
        additionCounter.increment(unmappedTarget);
      }
    }
    return deletions;
  }

  private Set<Symbol> computeAdditions(Array<SymbolWorkspace> parentWorkspaces, List<Symbol> targetSymbols,
                                       CountingMap<Symbol> additionCounter) {
    if (parentWorkspaces.isEmpty()) {
      return new HashSet<>(targetSymbols);
    } else {
      return AnStream.from(additionCounter)
        .filter(e -> e.getValue() == parentWorkspaces.length)
        .map(Map.Entry::getKey)
        .toSet();
    }
  }

}
