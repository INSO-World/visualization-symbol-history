package com.mategka.dava.analyzer.struct;

import com.mategka.dava.analyzer.extension.IndexMap;
import com.mategka.dava.analyzer.spoon.Spoon;
import com.mategka.dava.analyzer.struct.property.*;
import com.mategka.dava.analyzer.struct.symbol.Symbol;

import com.google.common.collect.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import spoon.reflect.CtModel;
import spoon.reflect.CtModelImpl;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtPackage;
import spoon.support.compiler.VirtualFile;

import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class StrandWorkspace {

  private static final String ROOT_PACKAGE_NAME = "ROOT";

  @NonNull
  @Getter
  final Strand strand;

  @Getter
  final BiMap<String, VirtualFile> spoonFiles = HashBiMap.create();

  @Getter
  final Map<VirtualFile, CtCompilationUnit> spoonUnits = new HashMap<>();

  final Map<String, Symbol> pathsToSymbols = new TreeMap<>();

  final IndexMap<Long, Symbol> idsToSymbols = new IndexMap<>(Symbol::getId);

  @Getter
  final Multimap<Symbol, Symbol> parentsToChildren = HashMultimap.create();

  CtModel model = Spoon.EMPTY_MODEL;

  public Symbol getPackage(CtPackage pakkage, SymbolCreationContext context) {
    var childPackagePath = Spoon.pathOf(pakkage);
    var childPackagePathString = childPackagePath.toString();
    var childPackagePathProperty = new PathProperty(childPackagePath);
    var existingSymbol = pathsToSymbols.get(childPackagePathString);
    if (existingSymbol != null) {
      return existingSymbol;
    }
    if (pakkage instanceof CtModelImpl.CtRootPackage) {
      var rootPackage = context.symbolBuilder()
        .property(new SimpleNameProperty(ROOT_PACKAGE_NAME))
        .property(KindProperty.Value.PACKAGE.toProperty())
        .property(childPackagePathProperty)
        .build();
      idsToSymbols.put(rootPackage);
      pathsToSymbols.put(childPackagePathString, rootPackage);
      return rootPackage;
    }
    var parentSymbol = getPackage(pakkage.getDeclaringPackage(), context);
    var childSymbol = context.symbolBuilder()
      .property(KindProperty.Value.PACKAGE.toProperty())
      .property(SimpleNameProperty.fromElement(pakkage))
      .property(ParentProperty.fromSymbol(parentSymbol))
      .property(childPackagePathProperty)
      .build();
    putSymbol(childSymbol);
    return childSymbol;
  }

  public void putSymbol(Symbol symbol) {
    idsToSymbols.put(symbol);
    pathsToSymbols.put(symbol.getPath().toString(), symbol);
    parentsToChildren.put(idsToSymbols.get(symbol.getParentId()), symbol);
  }

  public CtCompilationUnit getUnit(String filePath) {
    return spoonUnits.get(spoonFiles.get(filePath));
  }

  private void updateModel(CtModel model) {
    this.model = model;
  }

}
