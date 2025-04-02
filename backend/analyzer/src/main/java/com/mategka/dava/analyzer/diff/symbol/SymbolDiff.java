package com.mategka.dava.analyzer.diff.symbol;

import com.mategka.dava.analyzer.collections.*;
import com.mategka.dava.analyzer.collections.Stack;
import com.mategka.dava.analyzer.diff.file.FileMapping;
import com.mategka.dava.analyzer.extension.*;
import com.mategka.dava.analyzer.extension.option.Options;
import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.extension.struct.Pair;
import com.mategka.dava.analyzer.extension.struct.TreeNode;
import com.mategka.dava.analyzer.extension.struct.TreeOrder;
import com.mategka.dava.analyzer.extension.traitlike.Using;
import com.mategka.dava.analyzer.git.Hash;
import com.mategka.dava.analyzer.git.Repository;
import com.mategka.dava.analyzer.git.Side;
import com.mategka.dava.analyzer.spoon.AstComparator;
import com.mategka.dava.analyzer.spoon.CtEqPath;
import com.mategka.dava.analyzer.spoon.Spoon;
import com.mategka.dava.analyzer.struct.pipeline.Symbolizer;
import com.mategka.dava.analyzer.struct.property.BodyHashProperty;
import com.mategka.dava.analyzer.struct.property.PathProperty;
import com.mategka.dava.analyzer.struct.property.SimpleNameProperty;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;
import com.mategka.dava.analyzer.struct.property.value.Kind;
import com.mategka.dava.analyzer.struct.symbol.*;

import com.google.common.collect.BiMap;
import gumtree.spoon.builder.CtWrapper;
import gumtree.spoon.diff.Diff;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.support.compiler.VirtualFile;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
public class SymbolDiff {

  public SymbolMappingResult getMapping(Array<SymbolWorkspace> parentWorkspaces, FileMapping fileMapping,
                                        SymbolCreationContext context, Repository repository) {
    var targetWorkspace = deriveTargetWorkspace(parentWorkspaces, fileMapping, repository, context.hasStrandChange());
    Array<ManyToManyMap<@NotNull Symbol, @NotNull Symbol, @Nullable Void>> symbolMaps = Array.fromSupplier(
      parentWorkspaces.length, ManyToManyMap::new);
    // 1. Map packages to each other
    // TODO: For now, packages are mapped 1:1 and moves/renames are not detected
    for (var parentWorkspaceTuple : parentWorkspaces.withIndex()) {
      var parentIndex = parentWorkspaceTuple.left();
      var symbolMap = symbolMaps.get(parentIndex);
      var parentWorkspace = parentWorkspaceTuple.right();
      Queue<Pair<TreeNode<Symbol>, TreeNode<Symbol>>> packageMatchingQueue = new ArrayDeque<>();
      packageMatchingQueue.add(Pair.of(parentWorkspace.getTree(), targetWorkspace.getTree()));
      while (!packageMatchingQueue.isEmpty()) {
        var matchingPair = packageMatchingQueue.remove();
        symbolMap.put(matchingPair.left().value(), matchingPair.right().value(), null);
        Map<CtEqPath, TreeNode<Symbol>> sourceMap = getPathIndex(matchingPair.left());
        Map<CtEqPath, TreeNode<Symbol>> targetMap = getPathIndex(matchingPair.right());
        Set<CtEqPath> commonPaths = SetsX.intersection(sourceMap.keySet(), targetMap.keySet());
        for (CtEqPath commonPath : commonPaths) {
          packageMatchingQueue.add(Pair.of(sourceMap.get(commonPath), targetMap.get(commonPath)));
        }
      }
    }
    // 2. Map file symbols to each other
    var comparator = new AstComparator();
    for (var mapping : fileMapping.getMappings().mappings()) {
      if (FileMapping.isFileAddition(mapping) || mapping.isDeletion()) {
        continue;
      }
      var file = mapping.source();
      var parentIndex = file.parentIndex();
      var symbolMap = symbolMaps.get(parentIndex);
      var sourceWorkspace = parentWorkspaces.get(parentIndex);
      var oldPath = file.filePath();
      var newPath = mapping.target();
      var oldMainType = sourceWorkspace.getFileSpoonUnits().get(oldPath).getMainType();
      var newMainType = targetWorkspace.getFileSpoonUnits().get(newPath).getMainType();
      var astDiff = comparator.compare(oldMainType, newMainType);
      var astMappings = extractMappings(
        astDiff, oldMainType, newMainType, sourceWorkspace.pathSet(), targetWorkspace.pathSet());
      var oldLocations = sourceWorkspace.getLocatedSymbols();
      var newLocations = targetWorkspace.getLocatedSymbols();
      var astSymbolMappings = astMappings.entrySet().stream()
        .map(Pair::fromEntry)
        .map(Pair.mapping(CtEqPath::of))
        .map(Pair.mapping(oldLocations::get, newLocations::get))
        .map(Pair.mapping(TreeNode::value))
        .toList();
      for (var pair : astSymbolMappings) {
        symbolMap.put(pair.left(), pair.right(), null);
      }
    }
    // 3. Identify (global) deletions and additions
    var targetSymbols = ListsX.map(targetWorkspace.getLocatedSymbols().values(), TreeNode::value);
    var additionCounter = new CountingMap<Symbol>();
    Set<Symbol> deletions = new HashSet<>();
    for (var parentWorkspaceTuple : parentWorkspaces.withIndex()) {
      var parentIndex = parentWorkspaceTuple.left();
      var parentWorkspace = parentWorkspaceTuple.right();
      var symbolMap = symbolMaps.get(parentIndex);
      var sourceSymbols = ListsX.map(parentWorkspace.getLocatedSymbols().values(), TreeNode::value);
      deletions.addAll(symbolMap.getUnmappedSources(sourceSymbols));
      for (var unmappedTarget : symbolMap.getUnmappedTargets(targetSymbols)) {
        additionCounter.increment(unmappedTarget);
      }
    }
    Set<Symbol> additions;
    if (parentWorkspaces.isEmpty()) {
      additions = new HashSet<>(targetSymbols);
    } else {
      additions = AnStream.from(additionCounter)
        .filter(e -> e.getValue() == parentWorkspaces.length)
        .map(Map.Entry::getKey)
        .toSet();
    }
    // 4. Choose suitable Context for all target symbols based on all sources, set predecessors, create prop updates
    List<SymbolUpdate> updates = new ArrayList<>();
    Function<Symbol, List<ParentSymbol>> sourceCollector = (Symbol target) -> AnStream.fromIndexed(symbolMaps)
      .map(Pair.mappingLeft(m -> m.getByTarget(target)))
      .flatMap(p -> p.left().stream()
        .map(m -> new ParentSymbol(p.right(), m.source()))
      )
      .toList();
    Set<Symbol> movedTargetSymbols = new HashSet<>();
    for (var targetNode : Using.iterator(targetWorkspace.getTree(), TreeOrder.PREORDER)) {
      var targetSymbol = targetNode.value();
      if (additions.contains(targetSymbol)) {
        // Target is new, so there are no predecessors, and it is already in the additions set (no symbol update)
        targetSymbol.setContext(context.generateContext());
        continue;
      }
      /*
      No diff necessary if symbol could have been taken from parent unchanged
      (self or ancestor file symbol has an unchanged relationship with a source file with an identical parent index)

      Symbol ID can be kept if all mapped parents (others are addition links) share the same symbol ID
       */
      var sourceRecords = sourceCollector.apply(targetSymbol);
      assert !sourceRecords.isEmpty(); // If no sources existed, the target would be an addition, which gets handled above
      var parentContexts = AnStream.from(sourceRecords)
        .map(ParentSymbol::symbol)
        .mapOption(Symbol::getContext)
        .toList();
      var parentSymbolIds = AnStream.from(parentContexts)
        .map(Symbol.Context::key)
        .map(SymbolKey::symbolId)
        .toSet();
      long symbolId = parentSymbolIds.size() == 1
        ? IterablesX.getOnlyElement(parentSymbolIds)
        : context.symbolIdCounter().getAndIncrement();
      Hash symbolCommit = parentContexts.size() == 1 && !context.hasStrandChange()
        ? parentContexts.getFirst().commit()
        : context.commit();
      var symbolContext = new Symbol.Context(new SymbolKey(symbolId, context.strandId()), symbolCommit);
      targetSymbol.setContext(symbolContext);
      for (var sourceRecord : sourceRecords) {
        var parentIndex = sourceRecord.parentIndex();
        var unchangedSymbols = targetWorkspace.getUnchangedFromParent().get(parentIndex);
        var sourceSymbol = sourceRecord.symbol();
        if (unchangedSymbols.contains(sourceSymbol)) {
          continue;
        }
        PropertyMap propertyDiff = sourceSymbol.getProperties().diff(targetSymbol);
        if (propertyDiff.isEmpty()) {
          continue;
        }
        Set<UpdateFlag> flags = EnumSet.noneOf(UpdateFlag.class);
        if (propertyDiff.containsProperty(SimpleNameProperty.class)) {
          flags.add(UpdateFlag.RENAMED);
        }
        if (propertyDiff.containsProperty(BodyHashProperty.class)) {
          flags.add(UpdateFlag.BODY_UPDATED);
        }
        var sourceNode = parentWorkspaces.get(parentIndex).getLocatedSymbols().get(sourceSymbol.getPath());
        if (sourceNode.isRoot() != targetNode.isRoot()) {
          throw new IllegalStateException("Root nodes should match but do not");
        }
        if (!sourceNode.isRoot()) {
          var sourceParentSymbol = sourceNode.parent().getOrThrow().value();
          var targetParentSymbol = targetNode.parent().getOrThrow().value();
          var parentsAreMapped = symbolMaps.get(parentIndex).getByTarget(targetParentSymbol).stream()
            .map(Mapping::source)
            .anyMatch(sourceParentSymbol::equals);
          if (!parentsAreMapped) {
            flags.add(UpdateFlag.MOVED);
            movedTargetSymbols.add(targetSymbol);
          } else if (movedTargetSymbols.contains(targetParentSymbol)) {
            flags.add(UpdateFlag.MOVED_WITH_PARENT);
            movedTargetSymbols.add(targetSymbol);
          }
          if (isFileSymbol(sourceSymbol, sourceParentSymbol) && isFileSymbol(targetSymbol, targetParentSymbol)
            && sourceSymbol.getKind() != targetSymbol.getKind()) {
            flags.add(UpdateFlag.REPLACED);
          }
        }
        updates.add(new SymbolUpdate(sourceSymbol.getKey(), symbolContext, propertyDiff, flags));
      }
    }
    return new SymbolMappingResult(targetWorkspace, additions, deletions, updates);
  }

  private SymbolWorkspace deriveTargetWorkspace(Array<SymbolWorkspace> parentWorkspaces, FileMapping fileMapping,
                                                Repository repository, boolean breakCommit) {
    final var targetRoot = getTargetRoot(parentWorkspaces);
    final Map<String, TreeNode<Symbol>> fileSymbols = new TreeMap<>();
    final Map<String, CtCompilationUnit> fileSpoonUnits = new TreeMap<>();
    final Array<Set<Symbol>> unchangedFromParent = Array.fromSupplier(parentWorkspaces.length, HashSet::new);
    for (var targetFilePath : fileMapping.getMappings().targets()) {
      if (targetFilePath == null) {
        continue; // Ignore deletions
      }
      var sourceMappings = fileMapping.getMappings().getByTarget(targetFilePath);
      var unchangedSources = AnStream.from(sourceMappings)
        .filter(Mapping::isStatic)
        .map(Mapping::source)
        .toTypedArray();
      // NOTE: Since the subtree is unchanged for all unchangedSources, we can take any one of them
      var entryData = Options.getFirst(unchangedSources).<TargetEntry>switchMap()
        .some(file -> {
          var fileTree = parentWorkspaces.get(file.parentIndex()).getFileSymbols().get(file.filePath());
          var parentPackage = establishPackageHierarchyByName(targetRoot, fileTree);
          var spoonUnit = parentWorkspaces.get(file.parentIndex()).getFileSpoonUnits().get(file.filePath());
          // TODO: Check if copying the fileTree is really necessary (do we need the old node's parent() rel afterwards?)
          return new TargetEntry(parentPackage, fileTree.copy(), spoonUnit);
        })
        .none(() -> {
          var changedSource = sourceMappings.getFirst().metadata().diffEntry(); // must exist since target exists
          var newContents = repository.readFile(changedSource, Side.NEW).getSuccess().get();
          var virtualFile = new VirtualFile(targetFilePath, newContents);
          var spoonUnit = Spoon.parse(virtualFile);
          var parentPackage = establishPackageHierarchyByPath(targetRoot, spoonUnit);
          var fileTree = Symbolizer.symbolizeFileType(spoonUnit.getMainType(), parentPackage.value());
          return new TargetEntry(parentPackage, fileTree, spoonUnit);
        })
        .resolve();
      for (var file : unchangedSources) {
        for (var node : entryData.fileTree()) {
          unchangedFromParent.get(file.parentIndex()).add(node.value());
        }
      }
      entryData.packageNode().add(entryData.fileTree());
      fileSymbols.put(targetFilePath, entryData.fileTree());
      fileSpoonUnits.put(targetFilePath, entryData.spoonUnit());
    }
    Symbolizer.augmentParentProperty(targetRoot);
    Map<CtEqPath, TreeNode<Symbol>> locatedSymbols = targetRoot.stream()
      .map(Pair.fromRight(n -> n.value().getPath()))
      .collect(CollectorsX.pairsToMutableMap(TreeMap::new));
    return new SymbolWorkspace(targetRoot, fileSymbols, fileSpoonUnits, locatedSymbols, unchangedFromParent);
  }

  private TreeNode<Symbol> establishPackageHierarchyByName(TreeNode<Symbol> targetRoot, TreeNode<Symbol> fileNode) {
    var packageStack = new Stack<Symbol>();
    var currentPackageNode = fileNode.parent().getOrThrow();
    while (!currentPackageNode.isRoot()) {
      packageStack.push(currentPackageNode.value());
      currentPackageNode = currentPackageNode.parent().getOrThrow();
    }
    return establishPackageHierarchyByName(targetRoot, packageStack);
  }

  private TreeNode<Symbol> establishPackageHierarchyByName(TreeNode<Symbol> targetRoot, Stack<Symbol> packageStack) {
    var currentParent = targetRoot;
    while (!packageStack.isEmpty()) {
      var packageSymbol = packageStack.pop();
      final TreeNode<Symbol> finalCurrentParent = currentParent;
      currentParent = ListsX.find(
          currentParent.children(),
          m -> m.value().getName().equals(packageSymbol.getName()) && m.value().getKind() == Kind.PACKAGE
        )
        .getOrCompute(() -> finalCurrentParent.addByValue(packageSymbol.clone()));
    }
    return currentParent;
  }

  private TreeNode<Symbol> establishPackageHierarchyByPath(TreeNode<Symbol> targetRoot, CtCompilationUnit spoonUnit) {
    var spoonPackage = spoonUnit.getPackageDeclaration().getReference().getDeclaration();
    var packageStack = new Stack<CtPackage>();
    var currentPackage = spoonPackage;
    while (!Spoon.isRootPackage(currentPackage)) {
      packageStack.push(currentPackage);
      currentPackage = currentPackage.getDeclaringPackage();
    }
    // Set root path in case it is unset
    targetRoot.value().putProperty(PathProperty.fromElement(currentPackage));
    return establishPackageHierarchyByPath(targetRoot, packageStack);
  }

  private TreeNode<Symbol> establishPackageHierarchyByPath(TreeNode<Symbol> targetRoot, Stack<CtPackage> packageStack) {
    var currentParent = targetRoot;
    while (!packageStack.isEmpty()) {
      var spoonPackage = packageStack.pop();
      final TreeNode<Symbol> finalCurrentParent = currentParent;
      currentParent = ListsX.find(
          currentParent.children(),
          m -> m.value().getPath().equals(CtEqPath.of(spoonPackage)) && m.value().getKind() == Kind.PACKAGE
        )
        .getOrCompute(() -> {
          var properties = PropertyMap.builder()
            .property(Kind.PACKAGE.toProperty())
            .property(SimpleNameProperty.fromElement(spoonPackage))
            .property(PathProperty.fromElement(spoonPackage))
            .build();
          var packageSymbol = Symbol.withPropertyMap(properties);
          return finalCurrentParent.addByValue(packageSymbol);
        });
    }
    return currentParent;
  }

  private @NotNull BiMap<CtElement, CtElement> extractMappings(Diff astDiff, CtElement oldMainType,
                                                               CtElement newMainType, Set<CtEqPath> sourcePaths,
                                                               Set<CtEqPath> targetPaths) {
    var result = AnStream.from(astDiff.getMappingsComp().asSet())
      .map(m -> Pair.of(m.first, m.second))
      .filter(Pair.filtering(t -> !t.isRoot() && !t.getType().isEmpty()))
      .map(Pair.mapping(Spoon::getMetaElement))
      .filter(Pair.filtering(e -> !(e instanceof CtWrapper<?>)))
      .filter(Pair.filtering(
        e -> sourcePaths.contains(CtEqPath.of(e)),
        e -> targetPaths.contains(CtEqPath.of(e))
      ))
      .collect(CollectorsX.toBiMap());
    var mainTypeTarget = result.get(oldMainType);
    if (mainTypeTarget != newMainType) {
      result.forcePut(oldMainType, newMainType);
    }
    return result;
  }

  private @NotNull Map<CtEqPath, @NotNull TreeNode<Symbol>> getPathIndex(TreeNode<Symbol> matchingPair) {
    return AnStream.from(matchingPair.children())
      .allow(n -> n.value().getKind() == Kind.PACKAGE)
      .collect(Collectors.toMap(n -> n.value().getPath(), Function.identity()));
  }

  private TreeNode<Symbol> getTargetRoot(Array<SymbolWorkspace> parentWorkspaces) {
    var inheritedRoot = Options.getFirst(parentWorkspaces)
      .map(SymbolWorkspace::getTree)
      .map(TreeNode::value);
    var properties = PropertyMap.builder()
      .property(SimpleNameProperty.forRootPackage())
      .property(Kind.PACKAGE.toProperty())
      .property(inheritedRoot.map(s -> s.getProperty(PathProperty.class)).getOrElse(PathProperty.EMPTY))
      .build();
    return new TreeNode<>(Symbol.withPropertyMap(properties));
  }

  private boolean isFileSymbol(Symbol symbol, Symbol parentSymbol) {
    return parentSymbol.getKind() == Kind.PACKAGE && symbol.getKind() != Kind.PACKAGE;
  }

  private record TargetEntry(TreeNode<Symbol> packageNode, TreeNode<Symbol> fileTree, CtCompilationUnit spoonUnit) {

  }

}
