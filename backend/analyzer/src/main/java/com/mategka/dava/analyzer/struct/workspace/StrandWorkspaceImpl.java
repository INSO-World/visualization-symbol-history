package com.mategka.dava.analyzer.struct.workspace;

import com.mategka.dava.analyzer.collections.FastRecordCollection;
import com.mategka.dava.analyzer.collections.IndexMap;
import com.mategka.dava.analyzer.extension.AnStream;
import com.mategka.dava.analyzer.spoon.CtEqPath;
import com.mategka.dava.analyzer.spoon.Spoon;
import com.mategka.dava.analyzer.struct.FileEntry;
import com.mategka.dava.analyzer.struct.Strand;
import com.mategka.dava.analyzer.struct.property.ParentProperty;
import com.mategka.dava.analyzer.struct.property.PathProperty;
import com.mategka.dava.analyzer.struct.property.SimpleNameProperty;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;
import com.mategka.dava.analyzer.struct.property.value.Kind;
import com.mategka.dava.analyzer.struct.symbol.*;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import spoon.reflect.CtModel;
import spoon.reflect.CtModelImpl;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.support.compiler.VirtualFile;

import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class StrandWorkspaceImpl implements MutableStrandWorkspace {

  @NonNull
  @Getter
  final Strand strand;

  /**
   * A collection of registered files, including their Spoon values and root symbols.
   */
  final FastRecordCollection<FileEntry> fileEntries = new FastRecordCollection<>(FileEntry.class);

  /**
   * Maps Spoon paths to symbols.
   */
  final Map<CtEqPath, Symbol> pathsToSymbols = new TreeMap<>();

  /**
   * Maps symbol IDs to symbols.
   */
  final IndexMap<SymbolKey, Symbol> keysToSymbols = new IndexMap<>(Symbol::getKey);

  final Set<Symbol> innerPackageSymbols = new HashSet<>();

  /**
   * Maps parent to child symbols.
   */
  final Multimap<SymbolKey, Symbol> parentsToChildren = HashMultimap.create();

  CtModel model = Spoon.EMPTY_MODEL;

  @Override
  public @NotNull Collection<FileEntry> getFileEntries() {
    return fileEntries;
  }

  /**
   * Retrieves the symbol for the given CtPackage, creating new symbols if it does not exist.
   * If the symbol does not exist, it and all missing package symbols along its
   * package hierarchy are created using the supplied creation context.
   *
   * @param pakkage the CtPackage for which to retrieve the Symbol
   * @param context data for how to create new package symbols where applicable
   * @return the corresponding package symbol, which might have been just created
   */
  @Override
  public @NotNull Symbol getPackage(@NotNull CtPackage pakkage, @NotNull SymbolCreationContext context) {
    var pakkagePath = CtEqPath.of(pakkage);
    var pakkagePathProperty = new PathProperty(pakkagePath);
    var existingSymbol = pathsToSymbols.get(pakkagePath);
    if (existingSymbol != null) {
      return existingSymbol;
    }
    if (pakkage instanceof CtModelImpl.CtRootPackage) {
      var properties = PropertyMap.builder()
        .property(SimpleNameProperty.forRootPackage())
        .property(Kind.PACKAGE.toProperty())
        .property(pakkagePathProperty)
        .build();
      var rootPackage = new BareSymbol(properties).complete(context);
      keysToSymbols.put(rootPackage);
      pathsToSymbols.put(pakkagePath, rootPackage);
      return rootPackage;
    }
    var parentSymbol = getPackage(pakkage.getDeclaringPackage(), context);
    var properties = PropertyMap.builder()
      .property(Kind.PACKAGE.toProperty())
      .property(SimpleNameProperty.fromElement(pakkage))
      .property(ParentProperty.fromSymbol(parentSymbol))
      .property(pakkagePathProperty)
      .build();
    var childSymbol = new BareSymbol(properties).complete(context);
    putSymbol(childSymbol);
    innerPackageSymbols.add(childSymbol);
    return childSymbol;
  }

  /**
   * Fetches the symbol at the given Spoon path.
   *
   * @param path a wrapped Spoon path
   * @return the symbol at the given location, or null if no such symbol exists
   */
  @Override
  public Symbol getSymbol(CtEqPath path) {
    return pathsToSymbols.get(path);
  }

  /**
   * Fetches the symbol corresponding to the given Spoon element.
   *
   * @param element the element whose path is used to query for the symbol
   * @return the corresponding symbol, or null if no such symbol exists
   */
  @Override
  public Symbol getSymbol(@NotNull CtElement element) {
    return getSymbol(CtEqPath.of(element));
  }

  /**
   * Retrieves a stream of all registered symbols present in the file at the given path.
   * The file under the given path has to have been registered beforehand.
   *
   * @param filePath a repository-local path to a known file
   * @return a stream of symbols in preorder (parents before children)
   */
  @Override
  public AnStream<Symbol> getSymbolsFromFilePath(@NotNull String filePath) {
    var typeSymbol = fileEntries.getWhere(FileEntry::gitPath, filePath).rootSymbol();
    return AnStream.cons(typeSymbol, getDescendantSymbols(typeSymbol));
  }

  /**
   * Retrieves the CtCompilationUnit for the file at the given path.
   *
   * @param filePath a repository-local path to a known file
   * @return the cached Spoon parse result, or null if no matching file is registered
   */
  @Override
  public CtCompilationUnit getUnit(String filePath) {
    return fileEntries.getWhere(FileEntry::gitPath, filePath).spoonUnit();
  }

  /**
   * Performs a move operation from the old to the new symbol.
   * The symbols are expected to have matching symbol IDs.
   * Semantically equivalent to removing the old symbol and adding the new one,
   * this shorthand is slightly more efficient if relative location remains unchanged.
   *
   * @param oldSymbol the old state of the symbol
   * @param newSymbol the new state of the symbol
   */
  @Override
  public void moveSymbol(@NotNull Symbol oldSymbol, @NotNull Symbol newSymbol) {
    keysToSymbols.put(newSymbol);
    pathsToSymbols.remove(oldSymbol.getPath());
    pathsToSymbols.put(newSymbol.getPath(), newSymbol);
    parentsToChildren.removeAll(oldSymbol.getKey());
    var oldParentKey = oldSymbol.getParentKey();
    var newParentKey = newSymbol.getParentKey();
    if (!oldParentKey.equals(newParentKey) && parentsToChildren.containsKey(oldSymbol.getParentKey())) {
      parentsToChildren.remove(oldSymbol.getParentKey(), oldSymbol);
    }
    parentsToChildren.put(newSymbol.getParentKey(), newSymbol);
  }

  /**
   * Iteratively removes all empty packages until only packages with (class) files are left.
   *
   * @return a list of all removed package symbols
   */
  @Override
  public @NotNull List<Symbol> purgeEmptyPackages() {
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

  /**
   * Registers an entry for a file and adds its root symbol.
   *
   * @param entry the entry for the new file
   */
  @Override
  public void putFileEntry(@NotNull FileEntry entry) {
    putSymbol(entry.rootSymbol());
    fileEntries.add(entry);
  }

  /**
   * Adds the given symbol to this workspace.
   *
   * @param symbol a symbol with a valid ParentProperty set
   */
  @Override
  public void putSymbol(@NotNull Symbol symbol) {
    keysToSymbols.put(symbol);
    pathsToSymbols.put(symbol.getPath(), symbol);
    parentsToChildren.put(symbol.getParentKey(), symbol);
  }

  /**
   * Removes the file entry and all descendant symbols corresponding to the given (class) symbol.
   *
   * @param symbol the root symbol of the file to remove
   */
  @Override
  public void removeClassSymbolHierarchy(@NotNull Symbol symbol) {
    removeSymbol(symbol);
    fileEntries.removeWhere(FileEntry::rootSymbol, symbol);
    getDescendantSymbols(symbol).forEach(this::removeSymbol);
  }

  /**
   * Removes the given symbol from this workspace.
   *
   * @param symbol the symbol to remove
   */
  @Override
  public void removeSymbol(Symbol symbol) {
    keysToSymbols.removeByValue(symbol);
    pathsToSymbols.remove(symbol.getPath());
    parentsToChildren.removeAll(symbol.getKey());
    if (parentsToChildren.containsKey(symbol.getParentKey())) {
      parentsToChildren.remove(symbol.getParentKey(), symbol);
    }
  }

  /**
   * Updates the VirtualFile and CtCompilationUnit for the file at the given path.
   *
   * @param gitPath   a repository-local path to the target file
   * @param spoonFile the new VirtualFile corresponding to the target file
   * @param spoonUnit the new CtCompilationUnit corresponding to the target file
   */
  @Override
  public void updateFileEntry(@NotNull String gitPath, @NotNull VirtualFile spoonFile,
                              @NotNull CtCompilationUnit spoonUnit) {
    fileEntries.computeWhere(
      FileEntry::gitPath,
      gitPath,
      r -> new FileEntry(r.gitPath(), spoonFile, spoonUnit, r.rootSymbol())
    );
  }

  /**
   * Updates the root symbol for the file with the given current root symbol.
   * When this method is called, {@link #updateFileEntry(String, VirtualFile, CtCompilationUnit)}
   * should also be called either before or after.
   *
   * @param oldSymbol the old root symbol
   * @param newSymbol the new root symbol
   */
  @Override
  public void updateFileEntry(@NotNull Symbol oldSymbol, @NotNull Symbol newSymbol) {
    fileEntries.computeWhere(
      FileEntry::rootSymbol,
      oldSymbol,
      r -> new FileEntry(r.gitPath(), r.spoonFile(), r.spoonUnit(), newSymbol)
    );
  }

  /**
   * Updates the full Spoon model.
   *
   * @param model the new model
   */
  @Override
  public void updateModel(CtModel model) {
    this.model = model;
  }

  /**
   * Returns a stream of all symbols from the subtree rooted at the given symbol.
   *
   * @param symbol the root symbol
   * @return a stream of the given symbol and all its known descendants in preorder (parents before children)
   */
  private AnStream<Symbol> getDescendantSymbols(@NotNull Symbol symbol) {
    var children = parentsToChildren.get(symbol.getKey());
    return AnStream.from(children).concat(children.stream().flatMap(this::getDescendantSymbols));
  }

}
