package com.mategka.dava.analyzer.diff.workspace;

import com.mategka.dava.analyzer.collections.Array;
import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.extension.struct.TreeNode;
import com.mategka.dava.analyzer.extension.struct.TreeOrder;
import com.mategka.dava.analyzer.struct.symbol.Symbol;

import lombok.Value;
import spoon.reflect.declaration.CtCompilationUnit;

import java.util.Map;
import java.util.SequencedCollection;
import java.util.Set;

@Value
public class SymbolWorkspace {

  TreeNode<Symbol> tree;

  Map<String, TreeNode<Symbol>> fileSymbols;

  Map<String, CtCompilationUnit> fileSpoonUnits;

  Map<String, TreeNode<Symbol>> locatedSymbols;

  Array<Set<Symbol>> unchangedFromParent;

  public SequencedCollection<Symbol> getAllSymbols() {
    return tree.stream(TreeOrder.PREORDER).map(TreeNode::value).toList();
  }

  public Set<Symbol> getSuccessions(Set<Symbol> additions) {
    return AnStream.from(locatedSymbols.values())
      .map(TreeNode::value)
      .filter(s -> !additions.contains(s))
      .toSet();
  }

  public Set<Symbol> getUnchangedFromParent(int index) {
    return unchangedFromParent.get(index);
  }

  public Symbol locateSymbol(String path) {
    return locatedSymbols.get(path).value();
  }

  public Set<String> pathSet() {
    return locatedSymbols.keySet();
  }

}
