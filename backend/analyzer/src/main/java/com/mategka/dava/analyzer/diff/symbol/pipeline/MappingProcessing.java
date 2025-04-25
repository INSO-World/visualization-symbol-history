package com.mategka.dava.analyzer.diff.symbol.pipeline;

import com.mategka.dava.analyzer.collections.Array;
import com.mategka.dava.analyzer.collections.ManyToManyMap;
import com.mategka.dava.analyzer.collections.Mapping;
import com.mategka.dava.analyzer.diff.symbol.ParentSymbol;
import com.mategka.dava.analyzer.diff.workspace.SymbolWorkspace;
import com.mategka.dava.analyzer.extension.IterablesX;
import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.extension.struct.Pair;
import com.mategka.dava.analyzer.extension.struct.TreeOrder;
import com.mategka.dava.analyzer.extension.traitlike.Using;
import com.mategka.dava.analyzer.git.Hash;
import com.mategka.dava.analyzer.struct.property.*;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;
import com.mategka.dava.analyzer.struct.property.value.Kind;
import com.mategka.dava.analyzer.struct.symbol.*;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@UtilityClass
public class MappingProcessing {

  public @NotNull List<SymbolUpdate> processSymbolMappings(SymbolWorkspace targetWorkspace,
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
      var sourceRecords = getAllSourceSymbols(symbolMaps, targetSymbol);
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
        var unchangedSymbols = targetWorkspace.getUnchangedFromParent(parentIndex);
        var sourceSymbol = sourceRecord.symbol();
        var sourceKey = sourceSymbol.getKey();
        if (collectingJoinPredecessors) {
          targetSymbol.addPredecessor(PrdRole.DIRECT, sourceKey);
        }
        if (unchangedSymbols.contains(targetSymbol)) {
          if (collectingMergePredecessors) {
            targetSymbol.addPredecessor(PrdRole.DIRECT, sourceKey);
          }
          continue;
        }
        if (collectingMergePredecessors) {
          targetSymbol.addPredecessor(PrdRole.MERGED, sourceKey);
        }
        PropertyMap.PropertyMapDiff propertyDiff = sourceSymbol.getProperties().diff(targetSymbol);
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
        updates.add(new SymbolUpdate(sourceSymbol.getKey(), symbolContext, propertyDiff.coalesce(), flags));
      }
    }
    return updates;
  }

  private static List<ParentSymbol> getAllSourceSymbols(
    Array<ManyToManyMap<@NotNull Symbol, @NotNull Symbol, @Nullable Void>> symbolMaps, Symbol target) {
    return AnStream.fromIndexed(symbolMaps)
      .map(Pair.mappingLeft(m -> m.getByTarget(target)))
      .flatMap(p -> p.left().stream()
        .map(m -> new ParentSymbol(p.right(), m.source()))
      )
      .toList();
  }

  private boolean isFileSymbol(Symbol symbol, Symbol parentSymbol) {
    return parentSymbol.getKind() == Kind.PACKAGE && symbol.getKind() != Kind.PACKAGE;
  }

}
