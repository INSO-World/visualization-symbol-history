package com.mategka.dava.analyzer.diff.workspace;

import com.mategka.dava.analyzer.collections.Array;
import com.mategka.dava.analyzer.extension.ListsX;
import com.mategka.dava.analyzer.extension.struct.TreeNode;
import com.mategka.dava.analyzer.spoon.CtEqPath;
import com.mategka.dava.analyzer.struct.symbol.Symbol;

import lombok.Value;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;

import java.util.Map;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class SymbolWorkspace {

  TreeNode<Symbol> tree;

  Map<String, TreeNode<Symbol>> fileSymbols;

  Map<String, CtCompilationUnit> fileSpoonUnits;

  Map<CtEqPath, TreeNode<Symbol>> locatedSymbols;

  Array<Set<Symbol>> unchangedFromParent;

  public SequencedCollection<Symbol> getAllSymbols() {
    return ListsX.map(locatedSymbols.values(), TreeNode::value);
  }

  public Set<Symbol> getSuccessions(Set<Symbol> additions) {
    return locatedSymbols.values().stream()
      .map(TreeNode::value)
      .filter(s -> !additions.contains(s))
      .collect(Collectors.toSet());
  }

  public Set<Symbol> getUnchangedFromParent(int index) {
    return unchangedFromParent.get(index);
  }

  public Symbol locateSymbol(CtElement element) {
    return locatedSymbols.get(CtEqPath.of(element)).value();
  }

  public Set<CtEqPath> pathSet() {
    return locatedSymbols.keySet();
  }

}
