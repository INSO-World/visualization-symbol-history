package com.mategka.dava.analyzer.diff.symbol.pipeline;

import com.mategka.dava.analyzer.collections.Array;
import com.mategka.dava.analyzer.collections.ManyToManyMap;
import com.mategka.dava.analyzer.diff.workspace.SymbolWorkspace;
import com.mategka.dava.analyzer.extension.SetsX;
import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.extension.struct.Pair;
import com.mategka.dava.analyzer.extension.struct.TreeNode;
import com.mategka.dava.analyzer.struct.property.value.Kind;
import com.mategka.dava.analyzer.struct.symbol.Symbol;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
public class PackageMapping {

  public void mapPackageSymbols(Array<SymbolWorkspace> parentWorkspaces,
                                Array<ManyToManyMap<@NotNull Symbol, @NotNull Symbol, @Nullable Void>> symbolMaps,
                                SymbolWorkspace targetWorkspace) {
    // TODO: For now, packages are mapped 1:1 and moves/renames are not detected
    for (var parentWorkspaceTuple : parentWorkspaces.withIndex()) {
      var parentIndex = parentWorkspaceTuple.left();
      var symbolMap = symbolMaps.get(parentIndex);
      var parentWorkspace = parentWorkspaceTuple.right();
      Queue<Pair<TreeNode<Symbol>, TreeNode<Symbol>>> packageMatchingQueue = new ArrayDeque<>();
      packageMatchingQueue.add(Pair.of(parentWorkspace.getTree(), targetWorkspace.getTree()));
      while (!packageMatchingQueue.isEmpty()) {
        var matchingPair = packageMatchingQueue.remove();
        symbolMap.put(matchingPair.left().value(), matchingPair.right().value(), null);
        Map<String, TreeNode<Symbol>> sourceMap = getPathIndex(matchingPair.left());
        Map<String, TreeNode<Symbol>> targetMap = getPathIndex(matchingPair.right());
        Set<String> commonPaths = SetsX.intersection(sourceMap.keySet(), targetMap.keySet());
        for (String commonPath : commonPaths) {
          packageMatchingQueue.add(Pair.of(sourceMap.get(commonPath), targetMap.get(commonPath)));
        }
      }
    }
  }

  private @NotNull Map<String, @NotNull TreeNode<Symbol>> getPathIndex(TreeNode<Symbol> matchingPair) {
    return AnStream.from(matchingPair.children())
      .allow(n -> n.value().getKind() == Kind.PACKAGE)
      .collect(Collectors.toMap(n -> n.value().getSpoonPath(), Function.identity()));
  }

}
