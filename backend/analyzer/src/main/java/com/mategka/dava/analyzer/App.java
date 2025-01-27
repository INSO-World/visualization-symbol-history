package com.mategka.dava.analyzer;

import com.mategka.dava.analyzer.extension.*;
import com.mategka.dava.analyzer.git.*;
import com.mategka.dava.analyzer.spoon.AstComparator;
import com.mategka.dava.analyzer.spoon.Spoon;
import com.mategka.dava.analyzer.struct.History;
import com.mategka.dava.analyzer.struct.Strand;
import com.mategka.dava.analyzer.struct.StrandWorkspace;
import com.mategka.dava.analyzer.struct.SymbolCreationContext;
import com.mategka.dava.analyzer.wip.ReflectionContext;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import spoon.reflect.declaration.*;
import spoon.support.compiler.VirtualFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App {

  public static void main(String[] args) {
    System.out.println("Hello World!");
    // ?REPO
    // ?REPO
    try (RepositoryWrapper repository = RepositoryWrapper.open("?REPO")) {
      Ref mainBranch = repository.resolveRef("main").orElseThrow();
      var reflectionContext = new ReflectionContext();
      var history = History.emptyOfBranch(repository, mainBranch);
      var timeBefore = System.currentTimeMillis();
      var symbolIdCounter = new AtomicLong();
      int offset = 0;
      var comparator = new AstComparator();
      Map<Strand, StrandWorkspace> workspaces = new DefaultMap<>(HashMap::new, StrandWorkspace::new);
      Map<VirtualFile, CtCompilationUnit> filesToUnits = new HashMap<>();
      // TODO: Traverse commits in normal topological order for ~5% performance boost
      try (RevWalk walk = repository.commitsUpTo(mainBranch, CommitOrder.REVERSE_TOPOLOGICAL)) {
        for (RevCommit commit : walk) {
          var commitSha = commit.getId().getName();
          var strand = history.getStrandMapping().get(commitSha);
          var workspace = workspaces.get(strand);
          System.out.print(commitSha.substring(0, 6) + " ");
          if (++offset >= 12) {
            offset = 0;
            System.out.println();
          }
          var parent = Optionals.getFirst(commit.getParents());
          if (parent.isEmpty()) {
            var diffs = repository.initialCommitFilesOf(commit);
            var relevantDiffs = RelevantDiffs.extract(diffs);
            var additions = relevantDiffs.get(FileChangeType.ADDED);
            if (additions.isEmpty()) {
              // No relevant changes
              continue;
            }
            var currentContents = workspace.getFiles();
            for (var diff : additions) {
              var content = repository.readFile(diff, Side.NEW).getSuccess().orElseThrow();
              var file = new VirtualFile(content, diff.getNewPath());
              currentContents.put(file.getPath(), file);
            }
            for (var file : currentContents.values()) {
              filesToUnits.put(file, Spoon.parse(file));
            }
            // TODO: Process symbol additions
            continue;
          }
          var actualParent = parent.get();
          var parentStrand = history.getStrandMapping().get(actualParent.getId().getName());
          var parentWorkspace = workspaces.get(parentStrand);
          var parentFiles = parentWorkspace.getFiles();
          try (DiffFormatter formatter = repository.newFormatter()) {
            var diffs = formatter.scan(actualParent.getTree(), commit.getTree());
            var relevantDiffs = RelevantDiffs.extract(diffs);
            Map<String, VirtualFile> overrideFiles = getOverrides(relevantDiffs, repository);
            if (overrideFiles.isEmpty()) {
              // No relevant changes
              continue;
            }
            Map<String, VirtualFile> effectiveFiles = new ChainMap<>(overrideFiles, parentFiles);
            Map<VirtualFile, CtCompilationUnit> effectiveUnits = effectiveFiles.values().stream()
              .filter(Objects::nonNull)
              .collect(Collectors2.toMap(Spoon::parse));
            // TODO: Do not trust rename, move and copy hints from Git
            var derivativeDiffPairs = Stream.of(FileChangeType.RENAMED, FileChangeType.MOVED, FileChangeType.COPIED)
              .flatMap(t -> relevantDiffs.get(t).stream().map(d -> Pair.of(t, d)))
              .toList();
            var creationContext = new SymbolCreationContext(symbolIdCounter, commitSha);
            var symbolAdder = new SymbolAdder(creationContext);
            for (var diffPair : derivativeDiffPairs) {
              var type = diffPair.getLeft();
              var diff = diffPair.getRight();
              // Treat declared type as renamed symbol
              var oldUnit = filesToUnits.get(parentFiles.get(diff.getOldPath()));
              var newUnit = effectiveUnits.get(overrideFiles.get(diff.getNewPath()));
              var astDiff = comparator.compare(oldUnit.getMainType(), newUnit.getMainType());
              var editScript = astDiff.getRootOperations();
              var mappings = astDiff.getMappingsComp();
              int dummy = 1;
            }
            for (var diff : relevantDiffs.get(FileChangeType.MODIFIED)) {
              var oldUnit = filesToUnits.get(parentFiles.get(diff.getOldPath()));
              var newUnit = effectiveUnits.get(overrideFiles.get(diff.getNewPath()));
              var astDiff = comparator.compare(oldUnit.getMainType(), newUnit.getMainType());
              var editScript = astDiff.getRootOperations();
              var mappings = astDiff.getMappingsComp();
              int dummy = 1;
            }
            for (var diff : relevantDiffs.get(FileChangeType.ADDED)) {
              var newUnit = effectiveUnits.get(overrideFiles.get(diff.getNewPath()));
              var packageDeclaration = newUnit.getPackageDeclaration().getReference().getDeclaration();
              var pakkage = workspaces.get(strand).getPackage(packageDeclaration, creationContext);
              var typeDeclaration = newUnit.getMainType();
              var addedSymbols = symbolAdder.parseTypeDeclaration(typeDeclaration, pakkage);
              int dummy = 1;
            }
            for (var diff : relevantDiffs.get(FileChangeType.DELETED)) {
              var oldUnit = filesToUnits.get(parentFiles.get(diff.getOldPath()));
              var typeDeclaration = oldUnit.getMainType();
              int dummy = 1;
            }
            for (var overrideFile : overrideFiles.entrySet()) {
              var path = overrideFile.getKey();
              var newFile = overrideFile.getValue();
              if (newFile == null) {
                filesToUnits.remove(parentFiles.get(path));
              } else {
                filesToUnits.put(newFile, effectiveUnits.get(newFile));
              }
            }
          }
        }
      }
      System.out.println("Done!");
      var time = System.currentTimeMillis() - timeBefore;
      System.out.println("Time (ms): " + time);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    // Goal 1: Get all changes for one commit
    // Goal 2: Get all changes for history (starting from specified commit)
    // Goal 3: Get all changes and refactorings for history
    // Goal 4: Get all symbol changes for history
    // Goal 5: Get all structured symbol changes for history (symbol parentage, ...)
    // Side Goal: Make sure each commit-file combo is only read and parsed ONCE

    /*
    Procedure:
    1) Get all file changes
    2) For added files, parse the contents and add all symbols (package symbols may already exist)
    3) For deleted files, mark all symbols associated with the file as deleted (recurs. delete packages if empty now)
    4) For modified files, parse the contents, retrieve the previous parsed contents, diff, then add/remove accordingly
    5) For moved and renamed files, proceed as with modifications, then:
    5a) If no semantic changes (only package and import changes): "true" move or rename (only move or rename)
    5b) If semantic changes: move/rename + add/remove symbols accordingly
    5z) Add new package symbols if applicable, recursively delete package symbols if empty now
    6) For copied files, proceed as with modifications, then:
    6a) If no semantic changes: "true" copy (simply copy currently known symbols, add package if applicable)
    6b) If semantic changes: treat new file like an addition (add symbols, add package if applicable)
     */
  }

  private static Map<String, VirtualFile> getOverrides(
    Map<FileChangeType, List<DiffEntry>> diffs,
    RepositoryWrapper repository
  ) {
    return diffs.entrySet().stream()
      .flatMap(e -> e.getValue().stream().map(d -> Pair.of(e.getKey(), d)))
      .flatMap(p -> {
        List<Pair<String, VirtualFile>> entries = new ArrayList<>();
        var type = p.getLeft();
        var diff = p.getRight();
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
      .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
  }

}
