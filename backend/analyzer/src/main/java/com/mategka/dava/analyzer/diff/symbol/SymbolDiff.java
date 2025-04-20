package com.mategka.dava.analyzer.diff.symbol;

import com.mategka.dava.analyzer.collections.*;
import com.mategka.dava.analyzer.diff.file.FileMapping;
import com.mategka.dava.analyzer.diff.workspace.SymbolWorkspace;
import com.mategka.dava.analyzer.extension.*;
import com.mategka.dava.analyzer.extension.option.Options;
import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.extension.struct.Pair;
import com.mategka.dava.analyzer.extension.struct.TreeNode;
import com.mategka.dava.analyzer.extension.struct.TreeOrder;
import com.mategka.dava.analyzer.extension.traitlike.Using;
import com.mategka.dava.analyzer.git.Hash;
import com.mategka.dava.analyzer.spoon.AstComparator;
import com.mategka.dava.analyzer.spoon.CtEqPath;
import com.mategka.dava.analyzer.spoon.Spoon;
import com.mategka.dava.analyzer.struct.property.*;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;
import com.mategka.dava.analyzer.struct.property.value.Kind;
import com.mategka.dava.analyzer.struct.symbol.*;

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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
public class SymbolDiff {

  public SymbolMappingResult getMapping(SymbolWorkspace targetWorkspace, Array<SymbolWorkspace> parentWorkspaces,
                                        FileMapping fileMapping,
                                        SymbolCreationContext context) {
    Array<ManyToManyMap<@NotNull Symbol, @NotNull Symbol, @Nullable Void>> symbolMaps = Array.fromSupplier(
      parentWorkspaces.length, ManyToManyMap::new);
    // 1. Map packages to each other
    step1AmapPackageSymbols(parentWorkspaces, symbolMaps, targetWorkspace);
    // 2. Map file symbols to each other
    step2AmapInnerSymbols(parentWorkspaces, fileMapping, symbolMaps, targetWorkspace);
    // 3. Identify (global) deletions and additions
    ExternalMappingSets externalMappings = step3AcomputeExternalMappings(parentWorkspaces, targetWorkspace, symbolMaps);
    // 4. Choose suitable Context for all target symbols based on all sources, set predecessors, create prop updates
    List<SymbolUpdate> updates = step4AprocessSymbolMappings(
      targetWorkspace, parentWorkspaces, context, externalMappings, symbolMaps);
    return new SymbolMappingResult(externalMappings.additions(), externalMappings.deletions(), updates);
  }

  private boolean isFileSymbol(Symbol symbol, Symbol parentSymbol) {
    return parentSymbol.getKind() == Kind.PACKAGE && symbol.getKind() != Kind.PACKAGE;
  }

  private static void step1AmapPackageSymbols(Array<SymbolWorkspace> parentWorkspaces,
                                              Array<ManyToManyMap<@NotNull Symbol, @NotNull Symbol, @Nullable Void>> symbolMaps,
                                              SymbolWorkspace targetWorkspace) {
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
        Map<CtEqPath, TreeNode<Symbol>> sourceMap = step1BgetPathIndex(matchingPair.left());
        Map<CtEqPath, TreeNode<Symbol>> targetMap = step1BgetPathIndex(matchingPair.right());
        Set<CtEqPath> commonPaths = SetsX.intersection(sourceMap.keySet(), targetMap.keySet());
        for (CtEqPath commonPath : commonPaths) {
          packageMatchingQueue.add(Pair.of(sourceMap.get(commonPath), targetMap.get(commonPath)));
        }
      }
    }
  }

  private @NotNull Map<CtEqPath, @NotNull TreeNode<Symbol>> step1BgetPathIndex(TreeNode<Symbol> matchingPair) {
    return AnStream.from(matchingPair.children())
      .allow(n -> n.value().getKind() == Kind.PACKAGE)
      .collect(Collectors.toMap(n -> n.value().getPath(), Function.identity()));
  }

  private static void step2AmapInnerSymbols(Array<SymbolWorkspace> parentWorkspaces, FileMapping fileMapping,
                                            Array<ManyToManyMap<@NotNull Symbol, @NotNull Symbol, @Nullable Void>> symbolMaps,
                                            SymbolWorkspace targetWorkspace) {
    var comparator = new AstComparator();
    for (var mapping : fileMapping.getMappings().mappings()) {
      // TODO: The deletion check should be redundant?
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

      // NOTE: This shortcut only works for copied file trees since elements such as methods are unordered
      if (mapping.isStatic() && oldMainType == newMainType) {
        for (var oldNode : sourceWorkspace.getFileSymbols().get(oldPath)) {
          var symbol = oldNode.value();
          symbolMap.put(symbol, symbol, null);
        }
        continue;
      }

      var astDiff = comparator.compare(oldMainType, newMainType);
      var astMappings = step2BextractMappings(
        astDiff, oldMainType, newMainType, sourceWorkspace.pathSet(), targetWorkspace.pathSet());
      var oldLocations = sourceWorkspace.getLocatedSymbols();
      var newLocations = targetWorkspace.getLocatedSymbols();
      var astSymbolMappings = astMappings.entrySet().stream()
        .map(Pair::fromEntry)
        .map(Pair.mapping(Id::value))
        .map(Pair.mapping(CtEqPath::of))
        .map(Pair.mapping(oldLocations::get, newLocations::get))
        .map(Pair.mapping(TreeNode::value))
        .toList();
      for (var pair : astSymbolMappings) {
        symbolMap.put(pair.left(), pair.right(), null);
      }
    }
  }

  private @NotNull BiMap<Id<CtElement>, Id<CtElement>> step2BextractMappings(Diff astDiff, CtElement oldMainType,
                                                                             CtElement newMainType,
                                                                             Set<CtEqPath> sourcePaths,
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
      .map(Pair.mapping(Id::of))
      .collect(CollectorsX.toBiMap());

    // Post Processing: If Spoon attempts to re-map the main type (e.g., to a nested class), override this behavior
    var mainTypeTarget = Options.fromNullable(result.get(Id.of(oldMainType))).map(Id::value).getOrNull();
    if (mainTypeTarget != newMainType) {
      result.forcePut(Id.of(oldMainType), Id.of(newMainType));
    }

    // Post Processing: Map leftover implicit elements based on simple name and type heuristics
    step2CmapImplicitElements(oldMainType, newMainType, sourcePaths, targetPaths, result);

    return result;
  }

  private static void step2CmapImplicitElements(CtElement oldMainType, CtElement newMainType, Set<CtEqPath> sourcePaths,
                                                Set<CtEqPath> targetPaths, BiMap<Id<CtElement>, Id<CtElement>> result) {
    var unmappedSourceElementIds = AnStream.<CtElement>from(oldMainType.getElements(null))
      .filter(e -> !(e instanceof CtWrapper<?>))
      .map(Pair.fromRight(CtEqPath::of))
      .filter(Pair.filteringLeft(sourcePaths::contains))
      .map(Pair.mappingRight(Id::of))
      .filter(Pair.filteringRight(id -> !result.containsKey(id)))
      .collect(CollectorsX.pairsToMap());
    var unmappedTargetElementIds = AnStream.<CtElement>from(newMainType.getElements(null))
      .filter(e -> !(e instanceof CtWrapper<?>))
      .map(Pair.fromRight(CtEqPath::of))
      .filter(Pair.filteringLeft(targetPaths::contains))
      .map(Pair.mappingRight(Id::of))
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
      step2DmapSimilarImplicitElements(result, unmappedSourceElementIds, identicalPaths, unmappedTargetElementIds);
    }
  }

  private static void step2DmapSimilarImplicitElements(BiMap<Id<CtElement>, Id<CtElement>> result,
                                                       Map<CtEqPath, Id<CtElement>> unmappedSourceElementIds,
                                                       Set<CtEqPath> identicalPaths,
                                                       Map<CtEqPath, Id<CtElement>> unmappedTargetElementIds) {
    Multimap<String, Id<CtElement>> unmappedSourceElementIdsByDiscriminator = step2EgetDiscriminatorMultimap(
      unmappedSourceElementIds, identicalPaths);
    Multimap<String, Id<CtElement>> unmappedTargetElementIdsByDiscriminator = step2EgetDiscriminatorMultimap(
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

  private static @NotNull Multimap<String, Id<CtElement>> step2EgetDiscriminatorMultimap(
    Map<CtEqPath, Id<CtElement>> unmappedElementIds, Set<CtEqPath> identicalPaths) {
    Multimap<String, Id<CtElement>> unmappedElementIdsByDiscriminator = HashMultimap.create();
    for (var unmappedElement : unmappedElementIds.entrySet()) {
      if (identicalPaths.contains(unmappedElement.getKey()) || !(unmappedElement.getValue()
        .value() instanceof CtNamedElement namedElement)) {
        continue;
      }
      var discriminator = "%s&%s&%s".formatted(
        unmappedElement.getKey().getPseudoParentString(), namedElement.getClass().getSimpleName(),
        namedElement.getSimpleName()
      );
      unmappedElementIdsByDiscriminator.put(discriminator, unmappedElement.getValue());
    }
    return unmappedElementIdsByDiscriminator;
  }

  private static @NotNull ExternalMappingSets step3AcomputeExternalMappings(Array<SymbolWorkspace> parentWorkspaces,
                                                                            SymbolWorkspace targetWorkspace,
                                                                            Array<ManyToManyMap<@NotNull Symbol, @NotNull Symbol, @Nullable Void>> symbolMaps) {
    var targetSymbols = ListsX.map(targetWorkspace.getLocatedSymbols().values(), TreeNode::value);
    var additionCounter = new CountingMap<Symbol>();
    Set<Symbol> deletions = step3BcomputeDeletions(parentWorkspaces, symbolMaps, targetSymbols, additionCounter);
    Set<Symbol> additions = step3CcomputeAdditions(parentWorkspaces, targetSymbols, additionCounter);
    return new ExternalMappingSets(deletions, additions);
  }

  private static @NotNull Set<Symbol> step3BcomputeDeletions(Array<SymbolWorkspace> parentWorkspaces,
                                                             Array<ManyToManyMap<@NotNull Symbol, @NotNull Symbol, @Nullable Void>> symbolMaps,
                                                             List<Symbol> targetSymbols,
                                                             CountingMap<Symbol> additionCounter) {
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
    return deletions;
  }

  private static Set<Symbol> step3CcomputeAdditions(Array<SymbolWorkspace> parentWorkspaces, List<Symbol> targetSymbols,
                                                    CountingMap<Symbol> additionCounter) {
    if (parentWorkspaces.isEmpty()) {
      return new HashSet<>(targetSymbols);
    } else {
      return AnStream.from(additionCounter)
        .filter(e -> e.getValue() == parentWorkspaces.length)
        .map(Map.Entry::getKey)
        .toSet();
    }
  }

  private static @NotNull List<SymbolUpdate> step4AprocessSymbolMappings(SymbolWorkspace targetWorkspace,
                                                                         Array<SymbolWorkspace> parentWorkspaces,
                                                                         SymbolCreationContext context,
                                                                         ExternalMappingSets externalMappings,
                                                                         Array<ManyToManyMap<@NotNull Symbol, @NotNull Symbol, @Nullable Void>> symbolMaps) {
    List<SymbolUpdate> updates = new ArrayList<>();
    Set<Symbol> movedTargetSymbols = new HashSet<>();
    for (var targetNode : Using.iterator(targetWorkspace.getTree(), TreeOrder.PREORDER)) {
      var targetSymbol = targetNode.value();
      //noinspection CodeBlock2Expr
      targetNode.parent().ifSome(parent -> {
        targetSymbol.putProperty(ParentProperty.fromSymbol(parent.value()));
      });
      if (externalMappings.additions().contains(targetSymbol)) {
        // Target is new, so there are no predecessors, and it is already in the additions set (no symbol update)
        targetSymbol.setContext(context.generateContext());
        continue;
      }
      /*
      No diff necessary if symbol could have been taken from parent unchanged
      (self or ancestor file symbol has an unchanged relationship with a source file with an identical parent index)

      Symbol ID can be kept if all mapped parents (others are addition links) share the same symbol ID
       */
      var sourceRecords = step4BgetAllSourceSymbols(symbolMaps, targetSymbol);
      assert !sourceRecords.isEmpty(); // If no sources existed, the target would be an addition, which gets handled above
      var parentContexts = AnStream.from(sourceRecords)
        .map(ParentSymbol::symbol)
        .mapOption(Symbol::getContext)
        .toList();
      var parentSymbolIds = AnStream.from(parentContexts)
        .map(Symbol.Context::key)
        .map(SymbolKey::symbolId)
        .toSet();
      var consistentSymbolId = parentSymbolIds.size() == 1;
      long symbolId = consistentSymbolId
        ? IterablesX.getOnlyElement(parentSymbolIds)
        : context.symbolIdCounter().getAndIncrement();
      Hash symbolCommit = parentContexts.size() == 1 && !context.hasStrandChange()
        ? parentContexts.getFirst().commit()
        : context.commit();
      var symbolContext = new Symbol.Context(new SymbolKey(symbolId, context.strandId()), symbolCommit);
      targetSymbol.setContext(symbolContext);
      var collectingJoinPredecessors = context.hasStrandChange() && consistentSymbolId;
      var collectingMergePredecessors = context.hasStrandChange() && !consistentSymbolId;
      for (var sourceRecord : sourceRecords) {
        var parentIndex = sourceRecord.parentIndex();
        var unchangedSymbols = targetWorkspace.getUnchangedFromParent().get(parentIndex);
        var sourceSymbol = sourceRecord.symbol();
        var sourceKey = sourceSymbol.getKey();
        if (collectingJoinPredecessors) {
          targetSymbol.addPredecessor(PrdRole.DIRECT, sourceKey);
        }
        if (unchangedSymbols.contains(sourceSymbol)) {
          if (collectingMergePredecessors) {
            targetSymbol.addPredecessor(PrdRole.DIRECT, sourceKey);
          }
          continue;
        }
        if (collectingMergePredecessors) {
          targetSymbol.addPredecessor(PrdRole.MERGED, sourceKey);
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
          var atFile = isFileSymbol(sourceSymbol, sourceParentSymbol) && isFileSymbol(targetSymbol, targetParentSymbol);
          if (atFile && propertyDiff.containsProperty(KindProperty.class)) {
            flags.add(UpdateFlag.REPLACED);
          }
        }
        updates.add(new SymbolUpdate(sourceSymbol.getKey(), symbolContext, propertyDiff, flags));
      }
    }
    return updates;
  }

  private static List<ParentSymbol> step4BgetAllSourceSymbols(
    Array<ManyToManyMap<@NotNull Symbol, @NotNull Symbol, @Nullable Void>> symbolMaps, Symbol target) {
    return AnStream.fromIndexed(symbolMaps)
      .map(Pair.mappingLeft(m -> m.getByTarget(target)))
      .flatMap(p -> p.left().stream()
        .map(m -> new ParentSymbol(p.right(), m.source()))
      )
      .toList();
  }

  private record ExternalMappingSets(Set<Symbol> deletions, Set<Symbol> additions) {

  }

}
