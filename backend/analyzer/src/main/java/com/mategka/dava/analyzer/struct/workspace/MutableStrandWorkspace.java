package com.mategka.dava.analyzer.struct.workspace;

import com.mategka.dava.analyzer.struct.symbol.Symbol;
import com.mategka.dava.analyzer.struct.symbol.SymbolCreationContext;
import com.mategka.dava.analyzer.struct.symbol.SymbolUpdate;

import org.jetbrains.annotations.NotNull;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtPackage;
import spoon.support.compiler.VirtualFile;

import java.util.List;

public interface MutableStrandWorkspace extends StrandWorkspace {

  @NotNull Symbol getPackage(@NotNull CtPackage pakkage, @NotNull SymbolCreationContext context);

  /**
   * Performs a move operation from the old to the new symbol.
   * The symbols are expected to have matching symbol IDs.
   * Semantically equivalent to removing the old symbol and adding the new one,
   * this shorthand is slightly more efficient if relative location remains unchanged.
   *
   * @param oldSymbol the old state of the symbol
   * @param newSymbol the new state of the symbol
   */
  void moveSymbol(@NotNull Symbol oldSymbol, @NotNull Symbol newSymbol);

  /**
   * Iteratively removes all empty packages until only packages with (class) files are left.
   *
   * @return a list of all removed package symbols
   */
  @NotNull List<Symbol> purgeEmptyPackages();

  /**
   * Registers an entry for a file and adds its root symbol.
   *
   * @param entry      the entry for the new file
   * @param rootSymbol the new root symbol
   */
  void putFileEntry(@NotNull FileEntry entry, @NotNull Symbol rootSymbol);

  /**
   * Adds the given symbol to this workspace.
   *
   * @param symbol a symbol with a valid ParentProperty set
   */
  void putSymbol(@NotNull Symbol symbol);

  /**
   * Removes the file entry and all descendant symbols corresponding to the given (class) symbol.
   *
   * @param symbol the root symbol of the file to remove
   */
  void removeClassSymbolHierarchy(@NotNull Symbol symbol);

  /**
   * Removes the given symbol from this workspace.
   *
   * @param symbol the symbol to remove
   */
  void removeSymbol(Symbol symbol);

  /**
   * Updates the VirtualFile and CtCompilationUnit for the file at the given path.
   *
   * @param gitPath   a repository-local path to the target file
   * @param spoonFile the new VirtualFile corresponding to the target file
   * @param spoonUnit the new CtCompilationUnit corresponding to the target file
   */
  void updateFileEntry(@NotNull String gitPath, @NotNull VirtualFile spoonFile, @NotNull CtCompilationUnit spoonUnit);

  /**
   * Updates the full Spoon model.
   *
   * @param model the new model
   */
  void updateModel(CtModel model);

  void updateSymbol(@NotNull SymbolUpdate update);

}
