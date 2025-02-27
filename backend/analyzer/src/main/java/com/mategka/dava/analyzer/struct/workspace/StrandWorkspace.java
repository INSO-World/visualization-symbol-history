package com.mategka.dava.analyzer.struct.workspace;

import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.spoon.CtEqPath;
import com.mategka.dava.analyzer.struct.Strand;
import com.mategka.dava.analyzer.struct.symbol.Symbol;

import org.jetbrains.annotations.NotNull;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;

import java.util.Collection;

public interface StrandWorkspace {

  @NotNull Collection<FileEntry> getFileEntries();

  @NotNull Strand getStrand();

  /**
   * Fetches the symbol at the given Spoon path.
   *
   * @param path a wrapped Spoon path
   * @return the symbol at the given location, or null if no such symbol exists
   */
  Symbol getSymbol(CtEqPath path);

  /**
   * Fetches the symbol corresponding to the given Spoon element.
   *
   * @param element the element whose path is used to query for the symbol
   * @return the corresponding symbol, or null if no such symbol exists
   */
  Symbol getSymbol(@NotNull CtElement element);

  /**
   * Retrieves a stream of all registered symbols present in the file at the given path.
   * The file under the given path has to have been registered beforehand.
   *
   * @param filePath a repository-local path to a known file
   * @return a stream of symbols in preorder (parents before children)
   */
  AnStream<Symbol> getSymbolsFromFilePath(@NotNull String filePath);

  /**
   * Retrieves the CtCompilationUnit for the file at the given path.
   *
   * @param filePath a repository-local path to a known file
   * @return the cached Spoon parse result, or null if no matching file is registered
   */
  CtCompilationUnit getUnit(String filePath);

  Succession succeed(Strand strand);

}
