package com.mategka.dava.analyzer.struct.workspace;

import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.spoon.CtEqPath;
import com.mategka.dava.analyzer.struct.Strand;
import com.mategka.dava.analyzer.struct.symbol.Symbol;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;

import java.util.Collection;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class StrandWorkspaceView implements StrandWorkspace {

  @NonNull
  MutableStrandWorkspace workspace;

  @Override
  public @NotNull Collection<FileEntry> getFileEntries() {
    return workspace.getFileEntries();
  }

  @Override
  public @NotNull Strand getStrand() {
    return workspace.getStrand();
  }

  @Override
  public Symbol getSymbol(CtEqPath path) {
    return workspace.getSymbol(path);
  }

  @Override
  public Symbol getSymbol(@NotNull CtElement element) {
    return workspace.getSymbol(element);
  }

  @Override
  public AnStream<Symbol> getSymbolsFromFilePath(@NotNull String filePath) {
    return workspace.getSymbolsFromFilePath(filePath);
  }

  @Override
  public CtCompilationUnit getUnit(String filePath) {
    return workspace.getUnit(filePath);
  }

  @Override
  public Succession succeed(Strand strand) {
    return workspace.succeed(strand);
  }

}
