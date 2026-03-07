package com.mategka.dava.analyzer.diff.symbol.pipeline;

import com.mategka.dava.analyzer.collections.Array;
import com.mategka.dava.analyzer.collections.DefaultMap;
import com.mategka.dava.analyzer.collections.ManyToManyMap;
import com.mategka.dava.analyzer.diff.file.FileMapping;
import com.mategka.dava.analyzer.diff.workspace.SymbolWorkspace;
import com.mategka.dava.analyzer.extension.*;
import com.mategka.dava.analyzer.extension.option.Options;
import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.extension.struct.Pair;
import com.mategka.dava.analyzer.spoon.AstComparator;
import com.mategka.dava.analyzer.spoon.Spoon;
import com.mategka.dava.analyzer.spoon.path.SpoonPaths;
import com.mategka.dava.analyzer.struct.symbol.Symbol;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import gumtree.spoon.builder.CtWrapper;
import gumtree.spoon.diff.Diff;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtNamedElement;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

@UtilityClass
public class IntraFileMapping {

  public void mapInnerSymbols(Array<SymbolWorkspace> parentWorkspaces, FileMapping fileMapping,
                              Array<ManyToManyMap<@NotNull Symbol, @NotNull Symbol, @Nullable Void>> symbolMaps,
                              SymbolWorkspace targetWorkspace, boolean breakCommit) {
    var comparator = new AstComparator();
    for (var mapping : fileMapping.getMappings().mappings()) {
      if (FileMapping.isFileAddition(mapping)) {
        continue;
      }
      var pathCache = new IdentityHashMap<CtElement, String>();
      var file = mapping.source();
      var parentIndex = file.parentIndex();
      var symbolMap = symbolMaps.get(parentIndex);
      var sourceWorkspace = parentWorkspaces.get(parentIndex);
      var oldPath = file.filePath();
      var newPath = mapping.target();
      var oldMainType = sourceWorkspace.getFileSpoonUnits().get(oldPath).getMainType();
      var newMainType = targetWorkspace.getFileSpoonUnits().get(newPath).getMainType();
      var newMainTypePath = SpoonPaths.getPath(newMainType, pathCache);

      // NOTE: This shortcut only works for copied file trees since elements such as methods are unordered
      if (mapping.isStatic() && targetWorkspace.getUnchangedFromParent(parentIndex)
        .contains(targetWorkspace.locateSymbol(newMainTypePath)) && !breakCommit) {
        for (var oldNode : sourceWorkspace.getFileSymbols().get(oldPath)) {
          var symbol = oldNode.value();
          symbolMap.put(symbol, symbol, null);
        }
        continue;
      }

      var astDiff = comparator.compare(oldMainType, newMainType);
      var astMappings = extractMappings(
        astDiff, oldMainType, newMainType, sourceWorkspace.pathSet(), targetWorkspace.pathSet(), pathCache);
      var astSymbolMappings = astMappings.entrySet().stream()
        .map(Pair::fromEntry)
        .map(Pair.mapping(Id::value))
        .map(Pair.mapping(e -> SpoonPaths.getPath(e, pathCache)))
        .map(Pair.mapping(sourceWorkspace::locateSymbol, targetWorkspace::locateSymbol))
        .toList();
      for (var pair : astSymbolMappings) {
        symbolMap.put(pair.left(), pair.right(), null);
      }
      pathCache.clear();
    }
  }

  private @NotNull BiMap<Id<CtElement>, Id<CtElement>> extractMappings(Diff astDiff, CtElement oldMainType,
                                                                       CtElement newMainType,
                                                                       Set<String> sourcePaths,
                                                                       Set<String> targetPaths,
                                                                       IdentityHashMap<CtElement, String> pathCache) {
    //noinspection MismatchedQueryAndUpdateOfCollection
    final Map<CtElement, Id<CtElement>> idCache = new DefaultMap<>(IdentityHashMap::new, Id::of);

    var result = AnStream.from(astDiff.getMappingsComp().asSet())
      .map(m -> Pair.of(m.first, m.second))
      .filter(Pair.filtering(t -> !t.isRoot() && !t.getType().isEmpty()))
      .map(Pair.mapping(Spoon::getMetaElement))
      .filter(Pair.filtering(e -> !(e instanceof CtWrapper<?>)))
      .filter(Pair.filtering(
        e -> sourcePaths.contains(SpoonPaths.getPath(e, pathCache)),
        e -> targetPaths.contains(SpoonPaths.getPath(e, pathCache))
      ))
      .map(Pair.mapping(idCache::get))
      .collect(CollectorsX.toBiMap());

    // Post Processing: If Spoon attempts to re-map the main type (e.g., to a nested class), override this behavior
    var mainTypeTarget = Options.fromNullable(result.get(idCache.get(oldMainType))).map(Id::value).getOrNull();
    if (mainTypeTarget != newMainType) {
      result.forcePut(idCache.get(oldMainType), idCache.get(newMainType));
    }

    // Post Processing: Map leftover implicit elements based on simple name and type heuristics
    mapImplicitElements(oldMainType, newMainType, sourcePaths, targetPaths, result, pathCache, idCache);
    idCache.clear();

    return result;
  }

  private @NotNull Multimap<String, Id<CtElement>> getDiscriminatorMultimap(
    Map<String, Id<CtElement>> unmappedElementIds, Set<String> identicalPaths) {
    Multimap<String, Id<CtElement>> unmappedElementIdsByDiscriminator = HashMultimap.create();
    for (var unmappedElement : unmappedElementIds.entrySet()) {
      if (identicalPaths.contains(unmappedElement.getKey()) || !(unmappedElement.getValue()
        .value() instanceof CtNamedElement namedElement)) {
        continue;
      }
      var discriminator = "%s&%s&%s".formatted(
        SpoonPaths.getParentPath(unmappedElement.getKey()), namedElement.getClass().getSimpleName(),
        namedElement.getSimpleName()
      );
      unmappedElementIdsByDiscriminator.put(discriminator, unmappedElement.getValue());
    }
    return unmappedElementIdsByDiscriminator;
  }

  private void mapImplicitElements(CtElement oldMainType, CtElement newMainType, Set<String> sourcePaths,
                                   Set<String> targetPaths, BiMap<Id<CtElement>, Id<CtElement>> result,
                                   IdentityHashMap<CtElement, String> pathCache,
                                   Map<CtElement, Id<CtElement>> idCache) {
    var unmappedSourceElementIds = AnStream.<CtElement>from(oldMainType.getElements(null))
      .filter(e -> !(e instanceof CtWrapper<?>))
      .map(Pair.fromRight(e -> SpoonPaths.getPath(e, pathCache)))
      .filter(Pair.filteringLeft(sourcePaths::contains))
      .map(Pair.mappingRight(idCache::get))
      .filter(Pair.filteringRight(id -> !result.containsKey(id)))
      .collect(CollectorsX.pairsToMap());
    var unmappedTargetElementIds = AnStream.<CtElement>from(newMainType.getElements(null))
      .filter(e -> !(e instanceof CtWrapper<?>))
      .map(Pair.fromRight(e -> SpoonPaths.getPath(e, pathCache)))
      .filter(Pair.filteringLeft(targetPaths::contains))
      .map(Pair.mappingRight(idCache::get))
      .filter(Pair.filteringRight(id -> !result.containsValue(id)))
      .collect(CollectorsX.pairsToMap());
    var identicalPaths = SetsX.intersection(unmappedSourceElementIds.keySet(), unmappedTargetElementIds.keySet());
    for (var identicalPath : identicalPaths) {
      var unmappedSourceElementId = unmappedSourceElementIds.get(identicalPath);
      var unmappedTargetElementId = unmappedTargetElementIds.get(identicalPath);
      if (unmappedSourceElementId.value().isImplicit() || unmappedTargetElementId.value().isImplicit()) {
        result.put(unmappedSourceElementId, unmappedTargetElementId);
      }
    }
    if (identicalPaths.size() != unmappedSourceElementIds.size()
      || unmappedSourceElementIds.size() != unmappedTargetElementIds.size()) {
      mapSimilarImplicitElements(result, unmappedSourceElementIds, identicalPaths, unmappedTargetElementIds);
    }
  }

  private void mapSimilarImplicitElements(BiMap<Id<CtElement>, Id<CtElement>> result,
                                          Map<String, Id<CtElement>> unmappedSourceElementIds,
                                          Set<String> identicalPaths,
                                          Map<String, Id<CtElement>> unmappedTargetElementIds) {
    Multimap<String, Id<CtElement>> unmappedSourceElementIdsByDiscriminator = getDiscriminatorMultimap(
      unmappedSourceElementIds, identicalPaths);
    Multimap<String, Id<CtElement>> unmappedTargetElementIdsByDiscriminator = getDiscriminatorMultimap(
      unmappedTargetElementIds, identicalPaths);
    var identicalDiscriminators = SetsX.intersection(
      unmappedSourceElementIdsByDiscriminator.keySet(),
      unmappedTargetElementIdsByDiscriminator.keySet()
    );
    for (var identicalDiscriminator : identicalDiscriminators) {
      var sourceIds = unmappedSourceElementIdsByDiscriminator.get(identicalDiscriminator);
      var targetIds = unmappedTargetElementIdsByDiscriminator.get(identicalDiscriminator);
      if (sourceIds.size() == 1 && targetIds.size() == 1) {
        var sourceId = IterablesX.getFirst(sourceIds);
        var targetId = IterablesX.getFirst(targetIds);
        if (sourceId.value().isImplicit() || targetId.value().isImplicit()) {
          result.put(sourceId, targetId);
        }
      }
    }
  }

}
