package com.mategka.dava.analyzer;

import com.mategka.dava.analyzer.collections.Array;
import com.mategka.dava.analyzer.collections.CountingMap;
import com.mategka.dava.analyzer.diff.file.FileChange;
import com.mategka.dava.analyzer.diff.file.FileDiff;
import com.mategka.dava.analyzer.diff.file.FileMapping;
import com.mategka.dava.analyzer.diff.symbol.SymbolDiff;
import com.mategka.dava.analyzer.diff.workspace.SymbolWorkspace;
import com.mategka.dava.analyzer.diff.workspace.TargetWorkspace;
import com.mategka.dava.analyzer.extension.ListsX;
import com.mategka.dava.analyzer.extension.option.Options;
import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.git.*;
import com.mategka.dava.analyzer.serialization.Serializer;
import com.mategka.dava.analyzer.struct.CommitDiff;
import com.mategka.dava.analyzer.struct.History;
import com.mategka.dava.analyzer.struct.Strand;
import com.mategka.dava.analyzer.struct.symbol.SymbolCreationContext;
import com.mategka.dava.analyzer.util.Benchmark;

import com.google.common.graph.Graph;
import org.eclipse.jgit.lib.Ref;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class App {

  public static void main(String[] args) {
    // ?REPO
    // ?REPO
    // NOTE: Previous best profiling on analyzer repository took 99.4s with 93.12% = 92.6s Spoon time, and ~825MB memory
    // NOTE: Last profiling on analyzer repository took 84.3s with 94.46% = 79.6s Spoon time, and ~575MB memory
    try (Repository repository = Repository.open("?REPO")) {
      Ref mainBranch = repository.resolveRef("HEAD").getOrThrow();
      var benchmark = Benchmark.start();
      var history = History.emptyOfBranch(repository, mainBranch);
      var strandMapping = history.getStrandMapping();
      var symbolIdCounter = new AtomicLong();
      var treeDiffer = repository.newTreeDiffer();
      List<CommitInfo> commits = new ArrayList<>(64);
      CountingMap<@NotNull Long> workspaceCountdown = initWorkspaceCountdown(history.getStrandDag());
      Map<@NotNull Long, SymbolWorkspace> workspaces = new HashMap<>();
      try (CommitWalk commitWalk = repository.commitsUpTo(mainBranch, CommitOrder.REVERSE_TOPOLOGICAL)) {
        for (Commit commit : commitWalk) {
          var hash = commit.hash();
          commits.add(commit.info());
          System.out.println("-".repeat(10) + hash.abbreviated() + "-".repeat(10));
          var strand = strandMapping.get(hash);
          var strandId = strand.getId();
          var commitPaths = repository.readRelevantPaths(commit);
          var parents = commit.parents();
          var parentStrandIds = parents.stream()
            .map(Commit::hash)
            .map(strandMapping::get)
            .map(Strand::getId)
            .toList();
          var parentWorkspaces = AnStream.from(parentStrandIds)
            .map(workspaces::get)
            .toTypedArray();
          var breakCommit = Options.getFirst(parentStrandIds)
            .map(id -> id != strandId)
            .getOrElse(false);
          var context = new SymbolCreationContext(symbolIdCounter, strandId, hash, breakCommit);

          var fileMapping = extractFileMapping(commit, repository, treeDiffer);
          Array<Collection<String>> pathsPerParent = parentWorkspaces.stream()
            .map(SymbolWorkspace::getFileSymbols)
            .map(m -> (Collection<String>) m.keySet())
            .toTypedArray();
          fileMapping.addUnchangedMappings(pathsPerParent, commitPaths);
          var targetWorkspace = TargetWorkspace.create(parentWorkspaces, fileMapping, repository, breakCommit);
          var symbolMapping = SymbolDiff.getMapping(targetWorkspace, parentWorkspaces, fileMapping, context);
          workspaces.put(strandId, targetWorkspace);
          CommitDiff diff = CommitDiff.builder()
            .commitData(commit)
            .additions(symbolMapping.additions())
            .successions(symbolMapping.successions())
            .deletions(symbolMapping.deletions())
            .updates(symbolMapping.updates())
            .build();
          diff.printDebug();
          strand.getCommitDiffs().add(diff);

          if (breakCommit) {
            boolean changed = false;
            for (var parentStrandId : parentStrandIds) {
              var remainingSuccessors = workspaceCountdown.decrementAndGet(parentStrandId);
              if (remainingSuccessors == 0) {
                workspaces.remove(parentStrandId);
                workspaceCountdown.remove(parentStrandId);
                changed = true;
              }
            }
            if (changed) {
              System.gc();
            }
          }
        }
      }
      var time = benchmark.end();
      {
        //noinspection UnusedAssignment
        mainBranch = null;
        //noinspection UnusedAssignment
        benchmark = null;
        //noinspection UnusedAssignment
        symbolIdCounter = null;
        //noinspection UnusedAssignment
        treeDiffer = null;
        workspaceCountdown.clear();
        workspaces.clear();
        //noinspection UnusedAssignment
        workspaces = null;
        System.gc();
      }
      System.out.printf("Done in %.1f seconds%n", time.toMillis() / 1000d);
      Serializer.writeJson(history, commits, "result.json");
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
        .map(ListsX.collecting(Collectors.partitioningBy(c -> c.changeType() == FileChangeType.ADDED)))
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

  @SuppressWarnings("UnstableApiUsage")
  private static CountingMap<@NotNull Long> initWorkspaceCountdown(@NotNull Graph<Strand> strandDag) {
    var workspaceCountdown = new CountingMap<@NotNull Long>();
    for (var strand : strandDag.nodes()) {
      var successorCount = strandDag.successors(strand).size();
      if (successorCount > 0) {
        workspaceCountdown.put(strand.getId(), successorCount);
      }
    }
    return workspaceCountdown;
  }

}
