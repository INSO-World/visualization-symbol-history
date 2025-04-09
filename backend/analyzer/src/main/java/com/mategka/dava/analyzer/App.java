package com.mategka.dava.analyzer;

import com.mategka.dava.analyzer.collections.Array;
import com.mategka.dava.analyzer.diff.file.FileChange;
import com.mategka.dava.analyzer.diff.file.FileDiff;
import com.mategka.dava.analyzer.diff.file.FileMapping;
import com.mategka.dava.analyzer.diff.symbol.SymbolDiff;
import com.mategka.dava.analyzer.diff.workspace.SymbolWorkspace;
import com.mategka.dava.analyzer.diff.workspace.TargetWorkspace;
import com.mategka.dava.analyzer.extension.ListsX;
import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.git.*;
import com.mategka.dava.analyzer.struct.CommitDiff;
import com.mategka.dava.analyzer.struct.History;
import com.mategka.dava.analyzer.struct.Strand;
import com.mategka.dava.analyzer.struct.symbol.SymbolCreationContext;
import com.mategka.dava.analyzer.util.Benchmark;

import org.eclipse.jgit.lib.Ref;
import org.jetbrains.annotations.NotNull;

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
      Map<@NotNull Long, SymbolWorkspace> workspaces = new HashMap<>();
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
          var parentWorkspaces = AnStream.from(parents)
            .map(Commit::hash)
            .map(strandMapping::get)
            .map(Strand::getId)
            .map(workspaces::get)
            .toTypedArray();
          var breakCommit = !parents.isEmpty() && strandMapping.get(parents.getFirst().hash()) != strand;
          var context = new SymbolCreationContext(symbolIdCounter, strand.getId(), commit.hash(), breakCommit);

          var fileMapping = extractFileMapping(commit, repository, treeDiffer);
          Array<Collection<String>> pathsPerParent = parentWorkspaces.stream()
            .map(SymbolWorkspace::getFileSymbols)
            .map(m -> (Collection<String>) m.keySet())
            .toTypedArray();
          fileMapping.addUnchangedMappings(pathsPerParent, commitPaths);
          var targetWorkspace = TargetWorkspace.create(parentWorkspaces, fileMapping, repository);
          var symbolMapping = SymbolDiff.getMapping(targetWorkspace, parentWorkspaces, fileMapping, context);
          workspaces.put(strand.getId(), targetWorkspace);
          CommitDiff diff = CommitDiff.builder()
            .commitData(commit)
            .additions(symbolMapping.additions())
            .deletions(symbolMapping.deletions())
            .updates(symbolMapping.updates())
            .refactorings(Collections.emptyList())
            .build();
          strand.getCommitDiffs().add(diff);
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

}
