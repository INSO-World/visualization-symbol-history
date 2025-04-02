package com.mategka.dava.analyzer.diff.symbol;

import com.mategka.dava.analyzer.collections.Array;
import com.mategka.dava.analyzer.extension.struct.TreeNode;
import com.mategka.dava.analyzer.spoon.CtEqPath;
import com.mategka.dava.analyzer.struct.symbol.Symbol2;

import lombok.Value;
import spoon.reflect.declaration.CtCompilationUnit;

import java.util.Map;
import java.util.Set;

@Value
public class SymbolWorkspace {

  TreeNode<Symbol2> tree;

  Map<String, TreeNode<Symbol2>> fileSymbols;

  Map<String, CtCompilationUnit> fileSpoonUnits;

  Map<CtEqPath, TreeNode<Symbol2>> locatedSymbols;

  Array<Set<Symbol2>> unchangedFromParent;

  public Set<CtEqPath> pathSet() {
    return locatedSymbols.keySet();
  }

}
