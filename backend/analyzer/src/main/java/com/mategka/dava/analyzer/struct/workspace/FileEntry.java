package com.mategka.dava.analyzer.struct.workspace;

import spoon.reflect.declaration.CtCompilationUnit;
import spoon.support.compiler.VirtualFile;

public record FileEntry(String gitPath, VirtualFile spoonFile, CtCompilationUnit spoonUnit, long rootSymbol) {

}
