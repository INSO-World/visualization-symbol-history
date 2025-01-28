package com.mategka.dava.analyzer.struct;

import com.mategka.dava.analyzer.collections.ChainMap;
import com.mategka.dava.analyzer.collections.IndexMap;
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

  @NonNull
  @Getter
  final Strand strand;

  @Getter
  final BiMap<String, VirtualFile> spoonFiles = HashBiMap.create();

  @Getter
  final Map<VirtualFile, CtCompilationUnit> spoonUnits = new HashMap<>();

  @Getter
  final Multimap<VirtualFile, Symbol> filesToSymbols = HashMultimap.create();

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
        .property(new SimpleNameProperty(Symbol.ROOT_PACKAGE_NAME))
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
    putGlobalSymbol(childSymbol);
    return childSymbol;
  }

  public List<Symbol> purgeEmptyPackages() {
    List<Symbol> result = new ArrayList<>();
    while (true) {
      var emptyPackages = idsToSymbols.values().stream()
        .filter(s -> !parentsToChildren.containsKey(s))
        .filter(s -> !Symbol.isRootPackage(s))
        .toList();
      if (emptyPackages.isEmpty()) {
        break;
      }
      for (var emptyPackage : emptyPackages) {
        idsToSymbols.removeByValue(emptyPackage);
        pathsToSymbols.remove(emptyPackage.getPath().toString());
      }
      result.addAll(emptyPackages);
    }
    return result;
  }

  private void putGlobalSymbol(Symbol symbol) {
    idsToSymbols.put(symbol);
    pathsToSymbols.put(symbol.getPath().toString(), symbol);
    parentsToChildren.put(idsToSymbols.get(symbol.getParentId()), symbol);
  }

  public void putLocalSymbol(VirtualFile file, Symbol symbol) {
    putGlobalSymbol(symbol);
    filesToSymbols.put(file, symbol);
  }

  public CtCompilationUnit getUnit(String filePath) {
    return ChainMap.getOnce(spoonFiles, spoonUnits, filePath);
  }

  private void updateModel(CtModel model) {
    this.model = model;
  }

}
