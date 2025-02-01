package com.mategka.dava.analyzer.struct;

import com.mategka.dava.analyzer.struct.symbol.Symbol;

import spoon.reflect.declaration.CtCompilationUnit;
import spoon.support.compiler.VirtualFile;

public record FileEntry(String gitPath, VirtualFile spoonFile, CtCompilationUnit spoonUnit, Symbol rootSymbol) {

}
