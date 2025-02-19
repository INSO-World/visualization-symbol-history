package com.mategka.dava.analyzer.struct;

import com.mategka.dava.analyzer.collections.FastRecordCollection;
import com.mategka.dava.analyzer.collections.IndexMap;
import com.mategka.dava.analyzer.extension.MyStream;
import com.mategka.dava.analyzer.spoon.CtEqPath;
import com.mategka.dava.analyzer.spoon.Spoon;
import com.mategka.dava.analyzer.struct.property.ParentProperty;
import com.mategka.dava.analyzer.struct.property.PathProperty;
import com.mategka.dava.analyzer.struct.property.SimpleNameProperty;
import com.mategka.dava.analyzer.struct.property.value.Kind;
import com.mategka.dava.analyzer.struct.symbol.Symbol;
import com.mategka.dava.analyzer.struct.symbol.SymbolCreationContext;

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

@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class StrandWorkspace {

  @NonNull
  @Getter
  final Strand strand;

  final FastRecordCollection<FileEntry> fileEntries = new FastRecordCollection<>(FileEntry.class);

  /**
   * Maps Spoon paths to symbols.
   */
  final Map<CtEqPath, Symbol> pathsToSymbols = new TreeMap<>();

  /**
   * Maps symbol IDs to symbols.
   */
  final IndexMap<Symbol.Key, Symbol> keysToSymbols = new IndexMap<>(Symbol::getKey);

  final Set<Symbol> innerPackageSymbols = new HashSet<>();

  /**
   * Maps parent to child symbols.
   */
  final Multimap<Symbol.Key, Symbol> parentsToChildren = HashMultimap.create();

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
        .property(SimpleNameProperty.forRootPackage())
        .property(Kind.PACKAGE.toProperty())
        .property(childPackagePathProperty)
        .build();
      keysToSymbols.put(rootPackage);
      pathsToSymbols.put(childPackageEqPath, rootPackage);
      return rootPackage;
    }
    var parentSymbol = getPackage(pakkage.getDeclaringPackage(), context);
    var childSymbol = context.symbolBuilder()
      .property(Kind.PACKAGE.toProperty())
      .property(SimpleNameProperty.fromElement(pakkage))
      .property(ParentProperty.fromSymbol(parentSymbol))
      .property(childPackagePathProperty)
      .build();
    putSymbol(childSymbol);
    innerPackageSymbols.add(childSymbol);
    return childSymbol;
  }

  public List<Symbol> purgeEmptyPackages() {
    List<Symbol> result = new ArrayList<>();
    while (true) {
      var emptyPackages = innerPackageSymbols.stream()
        .filter(s -> !parentsToChildren.containsKey(s.getKey()))
        .toList();
      if (emptyPackages.isEmpty()) {
        break;
      }
      for (var emptyPackage : emptyPackages) {
        keysToSymbols.removeByValue(emptyPackage);
        pathsToSymbols.remove(emptyPackage.getPath());
        innerPackageSymbols.remove(emptyPackage);
      }
      result.addAll(emptyPackages);
    }
    return result;
  }

  public void replaceFileEntry(String gitPath, VirtualFile spoonFile, CtCompilationUnit spoonUnit) {
    fileEntries.computeWhere(
      FileEntry::gitPath,
      gitPath,
      r -> new FileEntry(r.gitPath(), spoonFile, spoonUnit, r.rootSymbol())
    );
  }

  public void putSymbol(Symbol symbol) {
    keysToSymbols.put(symbol);
    pathsToSymbols.put(CtEqPath.of(symbol.getPath()), symbol);
    parentsToChildren.put(symbol.getParentKey(), symbol);
  }

  public void putClassSymbol(FileEntry entry) {
    putSymbol(entry.rootSymbol());
    fileEntries.add(entry);
  }

  private void removeSymbol(Symbol symbol) {
    keysToSymbols.removeByValue(symbol);
    pathsToSymbols.remove(symbol.getPath());
    parentsToChildren.removeAll(symbol.getKey());
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

  public MyStream<Symbol> getSymbolsFromFilePath(String filePath) {
    var typeSymbol = fileEntries.getWhere(FileEntry::gitPath, filePath).rootSymbol();
    return MyStream.cons(typeSymbol, getDescendantSymbols(typeSymbol));
  }

  private MyStream<Symbol> getDescendantSymbols(Symbol symbol) {
    var children = parentsToChildren.get(symbol.getKey());
    return MyStream.from(children).concat(children.stream().flatMap(this::getDescendantSymbols));
  }

  private void updateModel(CtModel model) {
    this.model = model;
  }

}
