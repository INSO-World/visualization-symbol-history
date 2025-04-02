package com.mategka.dava.analyzer;

import com.mategka.dava.analyzer.collections.Array;
import com.mategka.dava.analyzer.diff.file.FileChange;
import com.mategka.dava.analyzer.diff.file.FileDiff;
import com.mategka.dava.analyzer.diff.file.FileMapping;
import com.mategka.dava.analyzer.diff.symbol.SymbolDiff;
import com.mategka.dava.analyzer.extension.CollectorsX;
import com.mategka.dava.analyzer.extension.ListsX;
import com.mategka.dava.analyzer.extension.option.Options;
import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.extension.struct.Pair;
import com.mategka.dava.analyzer.extension.struct.TreeNode;
import com.mategka.dava.analyzer.git.*;
import com.mategka.dava.analyzer.spoon.Spoon;
import com.mategka.dava.analyzer.spoon.action.*;
import com.mategka.dava.analyzer.struct.History;
import com.mategka.dava.analyzer.struct.symbol.Symbol;
import com.mategka.dava.analyzer.struct.symbol.SymbolCreationContext;
import com.mategka.dava.analyzer.struct.workspace.FileEntry;
import com.mategka.dava.analyzer.struct.workspace.StrandWorkspace;
import com.mategka.dava.analyzer.util.Benchmark;

import com.google.common.collect.BiMap;
import com.google.common.collect.Multimap;
import gumtree.spoon.builder.CtWrapper;
import gumtree.spoon.diff.Diff;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Ref;
import org.jetbrains.annotations.NotNull;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;
import spoon.support.compiler.VirtualFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class App {

  public static void main(String[] args) {
    System.out.println("Hello World!");
    // ?REPO
    // ?REPO
    try (Repository repository = Repository.open("?REPO")) {
      Ref mainBranch = repository.resolveRef("main").getOrThrow();
      var benchmark = Benchmark.start();
      var history = History.emptyOfBranch(repository, mainBranch);
      var strandMapping = history.getStrandMapping();
      var symbolIdCounter = new AtomicLong();
      int offset = 0;
      var treeDiffer = repository.newTreeDiffer();
      // TODO: Traverse commits in normal topological order for ~5% performance boost
      try (CommitWalk commitWalk = repository.commitsUpTo(mainBranch, CommitOrder.REVERSE_TOPOLOGICAL)) {
        for (Commit commit : commitWalk) {
          var strand = strandMapping.get(commit.hash());
          var commitPaths = repository.readRelevantPaths(commit);
          System.out.print(commit.hash().minimal() + " ");
          if (++offset >= 18) {
            offset = 0;
            System.out.println();
          }
          var parents = commit.parents();
          var breakCommit = !parents.isEmpty() && strandMapping.get(parents.getFirst().hash()) != strand;
          var context = new SymbolCreationContext(symbolIdCounter, strand.getId(), commit.hash(), breakCommit);

          var fileMapping = extractFileMapping(commit, repository, treeDiffer);
          Array<List<String>> pathsPerParent = null; // TODO: Retrieve from prior data once implemented
          fileMapping.addUnchangedMappings(pathsPerParent, commitPaths);
          // TODO: Store commitPaths for child commits

          Array<TreeNode<Symbol>> parentData = null;
          var symbolMapping = SymbolDiff.getMapping(fileMapping, parentData);
        }
      }
      System.out.println("Done!");
      var time = benchmark.end();
      System.out.println("Time (ms): " + time.toMillis());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static FileMapping extractFileMapping(Commit commit, Repository repository, TreeDiffer treeDiffer)
    throws IOException {
    var parents = commit.parents();
    Array<Map<String, FileChange>> relevantChangesPerParent;
    Array<Map<String, FileChange>> relevantAdditionsPerParent;
    if (parents.isEmpty()) {
      var relevantDiffs = RelevantDiffs.extract2(repository.initialCommitFilesOf(commit));
      var relevantAdditions = ListsX.collect(
        relevantDiffs, Collectors.toMap(FileChange::getNewPath, Function.identity()));
      relevantChangesPerParent = Array.of(Collections.emptyMap());
      relevantAdditionsPerParent = Array.of(relevantAdditions);
    } else {
      var allChangesPerParent = AnStream.from(parents)
        .map(p -> treeDiffer.diff(p, commit))
        .map(RelevantDiffs::extract2)
        .map(ListsX.collecting(Collectors.partitioningBy(c -> c.getOldPath() == null)))
        .toTypedArray();
      relevantChangesPerParent = AnStream.from(allChangesPerParent)
        .map(m -> m.get(false))
        .map(ListsX.collecting(Collectors.toMap(FileChange::getOldPath, Function.identity())))
        .toTypedArray();
      relevantAdditionsPerParent = AnStream.from(allChangesPerParent)
        .map(m -> m.get(true))
        .map(ListsX.collecting(Collectors.toMap(FileChange::getNewPath, Function.identity())))
        .toTypedArray();
    }
    return FileDiff.getMapping(relevantChangesPerParent, relevantAdditionsPerParent);
  }

  private static BiMap<CtElement, CtElement> extractMappings(Diff astDiff) {
    return AnStream.from(astDiff.getMappingsComp().asSet())
      .map(m -> Pair.of(m.first, m.second))
      .filter(Pair.filtering(t -> !t.isRoot() && !t.getType().isEmpty()))
      .map(Pair.mapping(Spoon::getMetaElement))
      .map(Pair.mapping(Options::fromNullable))
      .map(Pair.mapping(o -> o.map(e -> e instanceof CtWrapper<?> ? null : e)))
      .map(Options::flatten)
      .mapMulti(Options.yieldIfSome())
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
    Repository repository
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
