package com.mategka.dava.analyzer;

import com.mategka.dava.analyzer.collections.IndexMap;
import com.mategka.dava.analyzer.extension.AnStream;
import com.mategka.dava.analyzer.extension.CollectorsX;
import com.mategka.dava.analyzer.extension.Pair;
import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.git.*;
import com.mategka.dava.analyzer.spoon.AstComparator;
import com.mategka.dava.analyzer.spoon.Spoon;
import com.mategka.dava.analyzer.spoon.action.*;
import com.mategka.dava.analyzer.struct.CommitDiff;
import com.mategka.dava.analyzer.struct.FileEntry;
import com.mategka.dava.analyzer.struct.History;
import com.mategka.dava.analyzer.struct.property.ParentProperty;
import com.mategka.dava.analyzer.struct.property.Property;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;
import com.mategka.dava.analyzer.struct.symbol.*;
import com.mategka.dava.analyzer.struct.workspace.StrandWorkspace;
import com.mategka.dava.analyzer.struct.workspace.StrandWorkspaceIndex;
import com.mategka.dava.analyzer.util.Benchmark;

import com.google.common.collect.*;
import gumtree.spoon.builder.CtWrapper;
import gumtree.spoon.diff.Diff;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Ref;
import org.jetbrains.annotations.NotNull;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;
import spoon.support.compiler.VirtualFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class App {

  public static void main(String[] args) {
    System.out.println("Hello World!");
    // ?REPO
    // ?REPO
    try (RepositoryWrapper repository = RepositoryWrapper.open("?REPO")) {
      Ref mainBranch = repository.resolveRef("main").getOrThrow();
      var benchmark = Benchmark.start();
      var history = History.emptyOfBranch(repository, mainBranch);
      var symbolIdCounter = new AtomicLong();
      int offset = 0;
      var comparator = new AstComparator();
      var workspaces = new StrandWorkspaceIndex();
      // TODO: Traverse commits in normal topological order for ~5% performance boost
      try (CommitWalk commitWalk = repository.commitsUpTo(mainBranch, CommitOrder.REVERSE_TOPOLOGICAL)) {
        for (Commit commit : commitWalk) {
          var strand = history.getStrandMapping().get(commit.sha());
          var workspace = workspaces.get(strand);
          System.out.print(commit.shortSha() + " ");
          if (++offset >= 12) {
            offset = 0;
            System.out.println();
          }
          var parent = Option.getFirst(commit.parents());
          if (parent.isNone()) {
            // TODO: Fix initial commit variant algorithm
            /*var diffs = repository.initialCommitFilesOf(commit);
            var additions = RelevantDiffs.extract(diffs).get(FileChangeType.ADDED);
            if (additions.isEmpty()) {
              // No relevant changes
              continue;
            }
            var currentContents = workspace.getSpoonFiles();
            for (var diff : additions) {
              var content = repository.readFile(diff, Side.NEW).getSuccess().orElseThrow();
              var file = new VirtualFile(content, diff.getNewPath());
              currentContents.put(file.getPath(), file);
            }
            for (var file : currentContents.values()) {
              //workspace.getSpoonUnits().put(file, Spoon.parse(file));
            }
            */
            continue;
          }
          var actualParent = parent.getOrThrow();
          var parentStrand = history.getStrandMapping().get(actualParent.sha());
          var parentWorkspace = workspaces.getReadonly(parentStrand);
          try (DiffFormatter formatter = repository.newFormatter()) {
            var diffs = formatter.scan(actualParent.tree(), commit.tree());
            var relevantDiffs = RelevantDiffs.extract(diffs);
            Map<String, VirtualFile> overrideFiles = getOverrides(relevantDiffs.asMap(), repository);
            if (overrideFiles.isEmpty()) {
              // No relevant changes
              continue;
            }
            Map<String, CtCompilationUnit> effectiveUnits = getEffectiveUnits(parentWorkspace, overrideFiles);
            // TODO: Remove call after implementing RENAMED, MOVED and COPIED
            moveDerivativeDiffEntries(relevantDiffs);
            // TODO: Do not trust rename, move and copy hints from Git
            var derivativeDiffPairs = Stream.of(FileChangeType.RENAMED, FileChangeType.MOVED, FileChangeType.COPIED)
              .flatMap(t -> relevantDiffs.get(t).stream().map(d -> Pair.of(t, d)))
              .toList();
            var creationContext = new SymbolCreationContext(symbolIdCounter, strand.getId(), commit.sha());
            var symbolizer = new Symbolizer(creationContext);
            List<Symbol> additions = new ArrayList<>();
            List<Symbol> deletions = new ArrayList<>();
            var updates = new IndexMap<Long, SymbolUpdate>(HashMap::new, u -> u.getKey().symbolId());
            for (var diffPair : derivativeDiffPairs) {
              assert false; // TODO: Remove 10 xdiffs lines above and implement this
              var type = diffPair.left();
              var diff = diffPair.right();
              // Treat declared type as renamed symbol
              var oldUnit = parentWorkspace.getUnit(diff.getOldPath());
              var newFile = overrideFiles.get(diff.getNewPath());
              var newUnit = effectiveUnits.get(diff.getNewPath());
              var astDiff = comparator.compare(oldUnit.getMainType(), newUnit.getMainType());
              var editScript = astDiff.getRootOperations();
              var mappings = astDiff.getMappingsComp();
              int dummy = 1;
            }
            for (var diff : relevantDiffs.get(FileChangeType.MODIFIED)) {
              var oldUnit = parentWorkspace.getUnit(diff.getOldPath());
              var newFile = overrideFiles.get(diff.getNewPath());
              var newUnit = effectiveUnits.get(diff.getNewPath());
              var astDiff = comparator.compare(oldUnit.getMainType(), newUnit.getMainType());
              var mappings = extractMappings(astDiff);
              var actions = EditActions.fromDiff(astDiff, mappings);
              Table<Long, String, Property> updateProperties = HashBasedTable.create();
              SetMultimap<Long, UpdateFlag> updateFlags = Multimaps.newSetMultimap(
                new HashMap<>(),
                () -> EnumSet.noneOf(UpdateFlag.class)
              );
              for (var action : actions) {
                switch (action) {
                  case ReplacementAction replacementAction -> {
                    var oldSymbol = parentWorkspace.getSymbol(replacementAction.getOldSubject().getElement());
                    var newBareSymbol = PropertyCapture.parseElement(replacementAction.getNewSubject().getElement());
                    updateProperties.row(oldSymbol.getId()).putAll(oldSymbol.getProperties().diff(newBareSymbol));
                    var newParentSymbol = workspace.getSymbol(replacementAction.getNewSubject().getParent());
                    var newSymbol = newBareSymbol.asReplacementFor(oldSymbol, newParentSymbol);
                    updateFlags.put(oldSymbol.getId(), UpdateFlag.REPLACED);
                    workspace.updateFileEntry(oldSymbol, newSymbol);
                  }
                  case AdditionAction additionAction -> {
                    var newParentSymbol = workspace.getSymbol(additionAction.getNewParent());
                    var newSymbol = PropertyCapture.parseElement(additionAction.getNewElement())
                      .withProperty(ParentProperty.fromSymbol(newParentSymbol))
                      .complete(creationContext);
                    workspace.putSymbol(newSymbol);
                    additions.add(newSymbol);
                  }
                  case DeletionAction deletionAction -> {
                    var oldSymbol = parentWorkspace.getSymbol(deletionAction.getOldElement());
                    workspace.removeSymbol(oldSymbol);
                    deletions.add(oldSymbol);
                  }
                  case BodyUpdateAction bodyUpdateAction -> {
                    var oldSymbol = parentWorkspace.getSymbol(bodyUpdateAction.getOldSubject().getElement());
                    // TODO: Also allow body updates for unchanged symbols
                    updateFlags.put(oldSymbol.getId(), UpdateFlag.BODY_UPDATED);
                  }
                  case MoveAction moveAction -> {
                    var oldSymbol = parentWorkspace.getSymbol(moveAction.getOldSubject().getElement());
                    var newBareSymbol = PropertyCapture.parseElement(moveAction.getNewSubject().getElement());
                    updateProperties.row(oldSymbol.getId()).putAll(oldSymbol.getProperties().diff(newBareSymbol));
                    var newParentSymbol = workspace.getSymbol(moveAction.getNewSubject().getParent());
                    var newSymbol = newBareSymbol.asReplacementFor(oldSymbol, newParentSymbol);
                    workspace.moveSymbol(oldSymbol, newSymbol);
                    var newParentFlags = updateFlags.get(newParentSymbol.getId());
                    var newParentMoved = Stream.of(UpdateFlag.MOVED, UpdateFlag.MOVED_WITH_PARENT)
                      .anyMatch(newParentFlags::contains);
                    var moveFlag = newParentMoved ? UpdateFlag.MOVED_WITH_PARENT : UpdateFlag.MOVED;
                    updateFlags.put(oldSymbol.getId(), moveFlag);
                  }
                  case UpdateAction updateAction -> {
                    var oldSymbol = parentWorkspace.getSymbol(updateAction.getOldSubject().getElement());
                    var newBareSymbol = PropertyCapture.parseElement(updateAction.getNewSubject().getElement());
                    updateProperties.row(oldSymbol.getId()).putAll(oldSymbol.getProperties().diff(newBareSymbol));
                  }
                }
              }
              updateProperties.rowMap().entrySet().stream()
                .map(Pair::fromEntry)
                .map(Pair.mapping(id -> new SymbolKey(id, strand.getId()), PropertyMap::new))
                .map(Pair.folding((key, map) -> new SymbolUpdate(
                  key,
                  commit.sha(),
                  map,
                  updateFlags.get(key.symbolId())
                )))
                .forEach(updates::put);
              workspace.updateFileEntry(diff.getOldPath(), newFile, newUnit);
              int dummy = 1;
            }
            for (var diff : relevantDiffs.get(FileChangeType.ADDED)) {
              var newFile = overrideFiles.get(diff.getNewPath());
              var newUnit = effectiveUnits.get(diff.getNewPath());
              var packageDeclaration = newUnit.getPackageDeclaration().getReference().getDeclaration();
              var pakkage = workspaces.get(strand).getPackage(packageDeclaration, creationContext);
              var typeDeclaration = newUnit.getMainType();

              var symbols = symbolizer.symbolizeType(typeDeclaration, pakkage).toMutableList();
              additions.addAll(symbols);
              var classSymbol = symbols.removeFirst();
              workspace.putFileEntry(new FileEntry(diff.getNewPath(), newFile, newUnit, classSymbol));
              symbols.forEach(workspace::putSymbol);
            }
            for (var diff : relevantDiffs.get(FileChangeType.DELETED)) {
              var symbols = workspace.getSymbolsFromFilePath(diff.getOldPath()).toMutableList();
              deletions.addAll(symbols);
              var classSymbol = symbols.getFirst();
              workspace.removeClassSymbolHierarchy(classSymbol);
            }
            deletions.addAll(workspace.purgeEmptyPackages());
            var commitDiff = CommitDiff.builder()
              .commit(commit)
              .successions(Collections.emptyMap())
              .refactorings(Collections.emptyList())
              .additions(additions)
              .deletions(deletions)
              .updates(updates)
              .build();
            strand.getCommitDiffs().add(commitDiff);
          }
        }
      }
      System.out.println("Done!");
      var time = benchmark.end();
      System.out.println("Time (ms): " + time.toMillis());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static BiMap<CtElement, CtElement> extractMappings(Diff astDiff) {
    return AnStream.from(astDiff.getMappingsComp().asSet())
      .map(m -> Pair.of(m.first, m.second))
      .filter(Pair.filtering(t -> !t.isRoot() && !t.getType().isEmpty()))
      .map(Pair.mapping(Spoon::getMetaElement))
      .map(Pair.mapping(Option::fromNullable))
      .map(Pair.mapping(o -> o.map(e -> e instanceof CtWrapper<?> ? null : e)))
      .map(Option::pair)
      .mapMulti(Option.yieldIfSome())
      .collect(CollectorsX.toBiMap());
  }

  private static @NotNull Map<String, CtCompilationUnit> getEffectiveUnits(@NotNull StrandWorkspace parentWorkspace,
                                                                           @NotNull Map<String, VirtualFile> overrideFiles) {
    Map<String, CtCompilationUnit> effectiveUnits = new HashMap<>();
    for (FileEntry parentFile : parentWorkspace.getFileEntries()) {
      effectiveUnits.put(parentFile.gitPath(), parentFile.spoonUnit());
    }
    for (var overrideFile : overrideFiles.entrySet()) {
      if (overrideFile.getValue() == null) {
        effectiveUnits.remove(overrideFile.getKey());
      } else {
        effectiveUnits.put(overrideFile.getKey(), Spoon.parse(overrideFile.getValue()));
      }
    }
    return effectiveUnits;
  }

  private static Map<String, VirtualFile> getOverrides(
    Map<FileChangeType, Collection<DiffEntry>> diffs,
    RepositoryWrapper repository
  ) {
    return AnStream.from(diffs.entrySet())
      .flatMap(e -> e.getValue().stream().map(d -> Pair.of(e.getKey(), d)))
      .flatMap(p -> {
        List<Pair<String, VirtualFile>> entries = new ArrayList<>();
        var type = p.left();
        var diff = p.right();
        if (type.isRemovingOldResource()) {
          entries.add(Pair.of(diff.getOldPath(), null));
        }
        if (type.isAddingNewResource()) {
          var newContent = repository.readFile(diff, Side.NEW).getSuccess().orElseThrow();
          var newFile = new VirtualFile(newContent, diff.getNewPath());
          entries.add(Pair.of(diff.getNewPath(), newFile));
        }
        return entries.stream();
      })
      .collect(CollectorsX.pairsToMap());
  }

  private static void moveDerivativeDiffEntries(Multimap<FileChangeType, DiffEntry> relevantDiffs) {
    var movedDiffs = relevantDiffs.removeAll(FileChangeType.RENAMED);
    var actuallyMovedDiffs = relevantDiffs.removeAll(FileChangeType.MOVED);
    movedDiffs.addAll(actuallyMovedDiffs);
    relevantDiffs.putAll(FileChangeType.DELETED, movedDiffs);
    relevantDiffs.putAll(FileChangeType.ADDED, movedDiffs);
    var copiedDiffs = relevantDiffs.removeAll(FileChangeType.COPIED);
    relevantDiffs.putAll(FileChangeType.ADDED, copiedDiffs);
  }

}
