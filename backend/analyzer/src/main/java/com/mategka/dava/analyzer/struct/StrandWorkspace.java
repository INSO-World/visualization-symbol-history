package com.mategka.dava.analyzer.struct;

import com.mategka.dava.analyzer.collections.IndexMap;
import com.mategka.dava.analyzer.extension.StreamsX;
import com.mategka.dava.analyzer.spoon.CtEqPath;
import com.mategka.dava.analyzer.spoon.Spoon;
import com.mategka.dava.analyzer.struct.property.*;
import com.mategka.dava.analyzer.struct.symbol.Symbol;
import com.mategka.dava.analyzer.util.FastRecordCollection;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.*;
import lombok.experimental.FieldDefaults;
import spoon.reflect.CtModel;
import spoon.reflect.CtModelImpl;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtPackage;
import spoon.support.compiler.VirtualFile;

import java.util.*;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class StrandWorkspace {

  @NonNull
  @Getter
  final Strand strand;

  final FastRecordCollection<FileEntry> fileEntries = new FastRecordCollection<>(FileEntry.class);

//  /**
//   * Maps Git workspace paths to their associated Spoon file.
//   */
//  final BiMap<String, VirtualFile> spoonFiles = HashBiMap.create();
//
//  /**
//   * Maps Spoon files to their associated Spoon AST.
//   */
//  final Map<VirtualFile, CtCompilationUnit> spoonUnits = new HashMap<>();
//
//  /**
//   * Maps Spoon files to associated symbols.
//   */
//  final BiMap<VirtualFile, Symbol> filesToSymbols = HashBiMap.create();

  /**
   * Maps Spoon paths to symbols.
   */
  final Map<CtEqPath, Symbol> pathsToSymbols = new TreeMap<>();

  /**
   * Maps symbol IDs to symbols.
   */
  final IndexMap<Symbol.Key, Symbol> keysToSymbols = new IndexMap<>(Symbol::getKey);

  /**
   * Maps parent to child symbols.
   */
  final Multimap<Symbol, Symbol> parentsToChildren = HashMultimap.create();

  CtModel model = Spoon.EMPTY_MODEL;

  public Symbol getPackage(CtPackage pakkage, SymbolCreationContext context) {
    var childPackagePath = CtEqPath.of(pakkage);
    var childPackageEqPath = CtEqPath.of(childPackagePath);
    var childPackagePathProperty = new PathProperty(childPackagePath);
    var existingSymbol = pathsToSymbols.get(childPackageEqPath);
    if (existingSymbol != null) {
      return existingSymbol;
    }
    if (pakkage instanceof CtModelImpl.CtRootPackage) {
      var rootPackage = context.symbolBuilder()
        .property(new SimpleNameProperty(Symbol.ROOT_PACKAGE_NAME))
        .property(KindProperty.Value.PACKAGE.toProperty())
        .property(childPackagePathProperty)
        .build();
      keysToSymbols.put(rootPackage);
      pathsToSymbols.put(childPackageEqPath, rootPackage);
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

  public List<Symbol> purgeEmptyPackages() {
    List<Symbol> result = new ArrayList<>();
    while (true) {
      var emptyPackages = keysToSymbols.values().stream()
        .filter(s -> !parentsToChildren.containsKey(s))
        .filter(s -> !Symbol.isRootPackage(s))
        .toList();
      if (emptyPackages.isEmpty()) {
        break;
      }
      for (var emptyPackage : emptyPackages) {
        keysToSymbols.removeByValue(emptyPackage);
        pathsToSymbols.remove(emptyPackage.getPath());
      }
      result.addAll(emptyPackages);
    }
    return result;
  }

  public void putSymbol(Symbol symbol) {
    keysToSymbols.put(symbol);
    pathsToSymbols.put(CtEqPath.of(symbol.getPath()), symbol);
    parentsToChildren.put(keysToSymbols.get(symbol.getParentKey()), symbol);
  }

  public void putClassSymbol(FileEntry entry) {
    putSymbol(entry.rootSymbol());
    fileEntries.add(entry);
  }

  private void removeSymbol(Symbol symbol) {
    keysToSymbols.removeByValue(symbol);
    pathsToSymbols.remove(symbol.getPath());
    parentsToChildren.removeAll(symbol);
  }

  public void removeClassSymbolHierarchy(Symbol symbol) {
    removeSymbol(symbol);
    fileEntries.removeWhere(FileEntry::rootSymbol, symbol);
    getDescendantSymbols(symbol).forEach(this::removeSymbol);
  }

  public Map<String, VirtualFile> getSpoonFiles() {
    return fileEntries.getMap(FileEntry::gitPath, FileEntry::spoonFile);
  }

  public CtCompilationUnit getUnit(String filePath) {
    return fileEntries.getWhere(FileEntry::gitPath, filePath).spoonUnit();
  }

  public Stream<Symbol> getSymbolsFromFilePath(String filePath) {
    var typeSymbol = fileEntries.getWhere(FileEntry::gitPath, filePath).rootSymbol();
    return StreamsX.cons(typeSymbol, getDescendantSymbols(typeSymbol));
  }

  private Stream<Symbol> getDescendantSymbols(Symbol symbol) {
    var children = parentsToChildren.get(symbol);
    return Stream.concat(children.stream(), children.stream().flatMap(this::getDescendantSymbols));
  }

  private void updateModel(CtModel model) {
    this.model = model;
  }

}
