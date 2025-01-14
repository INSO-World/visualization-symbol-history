package com.mategka.dava.analyzer;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.mategka.dava.analyzer.git.*;
import com.mategka.dava.analyzer.struct.History;
import com.mategka.dava.analyzer.util.*;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.jetbrains.annotations.NotNull;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.support.compiler.VirtualFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App {

  public static void main(String[] args) {
    System.out.println("Hello World!");
    // ?REPO
    // ?REPO
    try (RepositoryWrapper repository = RepositoryWrapper.open("?REPO")) {
      Ref mainBranch = repository.resolveRef("main").orElseThrow();
      var history = History.emptyOfBranch(repository, mainBranch);
      var timeBefore = System.currentTimeMillis();
      int offset = 0;
      var comparator = new AstComparator();
      Table<Long, String, VirtualFile> pathsToLastContents = HashBasedTable.create();
      Map<VirtualFile, CtCompilationUnit> filesToUnits = new HashMap<>();
      Map<Long, CtModel> strandToModel = new HashMap<>();
      // TODO: Traverse commits in normal topological order for ~5% performance boost
      try (RevWalk walk = repository.commitsUpTo(mainBranch, CommitOrder.REVERSE_TOPOLOGICAL)) {
        for (RevCommit commit : walk) {
          var strandId = history.getStrandMapping().get(commit.getId().getName()).getId();
          System.out.print(commit.getId().getName().substring(0, 6) + " ");
          if (++offset >= 12) {
            offset = 0;
            System.out.println();
          }
          var parent = Optionals.getFirst(commit.getParents());
          if (parent.isEmpty()) {
            var diffs = repository.initialCommitFilesOf(commit);
            var relevantDiffs = selectRelevantChanges(diffs);
            var additions = relevantDiffs.get(FileChangeType.ADDED);
            if (additions.isEmpty()) {
              // No relevant changes
              strandToModel.put(strandId, Spoon.EMPTY_MODEL);
              continue;
            }
            var launcher = Spoon.newLauncher();
            for (var diff : additions) {
              var content = repository.readFile(diff, Side.NEW).getSuccess().orElseThrow();
              var file = new VirtualFile(content, diff.getNewPath());
              pathsToLastContents.put(strandId, file.getPath(), file);
              launcher.addInputResource(file);
            }
            var model = launcher.buildModel();
            strandToModel.put(strandId, model);
            var units = Spoon.getCompilationUnits(model);
            for (var unit : units) {
              var file = pathsToLastContents.get(strandId, Spoon.pathOf(unit));
              if (file != null) {
                filesToUnits.put(file, unit);
              }
            }
            // TODO: Process symbol additions
            continue;
          }
          var actualParent = parent.get();
          var parentStrandId = history.getStrandMapping().get(actualParent.getId().getName()).getId();
          try (DiffFormatter formatter = repository.newFormatter()) {
            var diffs = formatter.scan(actualParent.getTree(), commit.getTree());
            var relevantDiffs = selectRelevantChanges(diffs);
            Map<String, VirtualFile> overrideFiles = getOverrides(relevantDiffs, repository);
            if (overrideFiles.isEmpty()) {
              // No relevant changes
              strandToModel.computeIfAbsent(strandId, _id -> strandToModel.get(parentStrandId));
              continue;
            }
            var launcher = Spoon.newLauncher();
            Map<String, VirtualFile> effectiveFiles = new ChainMap<>(overrideFiles, pathsToLastContents.row(parentStrandId));
            effectiveFiles.values().stream()
              .filter(Objects::nonNull)
              .forEach(launcher::addInputResource);
            var model = launcher.buildModel();
            strandToModel.put(strandId, model);
            var units = Spoon.getCompilationUnits(model);
            Map<VirtualFile, CtCompilationUnit> newUnits = units.stream()
              .map(u -> Pair.of(effectiveFiles.get(Spoon.pathOf(u)), u))
              .filter(p -> p.getLeft() != null)
              .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
            // TODO: Do not trust rename, move and copy hints from Git
            var derivativeDiffPairs = Stream.of(FileChangeType.RENAMED, FileChangeType.MOVED, FileChangeType.COPIED)
              .flatMap(t -> relevantDiffs.get(t).stream().map(d -> Pair.of(t, d)))
              .toList();
            for (var diffPair : derivativeDiffPairs) {
              var type = diffPair.getLeft();
              var diff = diffPair.getRight();
              // Treat declared type as renamed symbol
              var oldUnit = filesToUnits.get(pathsToLastContents.get(parentStrandId, diff.getOldPath()));
              var newUnit = newUnits.get(overrideFiles.get(diff.getNewPath()));
              var astDiff = comparator.compare(oldUnit, newUnit);
              var editScript = astDiff.getRootOperations();
              var mappings = astDiff.getMappingsComp();
            }
            for (var diff : relevantDiffs.get(FileChangeType.MODIFIED)) {
              var oldUnit = filesToUnits.get(pathsToLastContents.get(parentStrandId, diff.getOldPath()));
              var newUnit = newUnits.get(overrideFiles.get(diff.getNewPath()));
              var astDiff = comparator.compare(oldUnit, newUnit);
              var editScript = astDiff.getRootOperations();
              var mappings = astDiff.getMappingsComp();
            }
            for (var diff : relevantDiffs.get(FileChangeType.ADDED)) {
              var newUnit = newUnits.get(overrideFiles.get(diff.getNewPath()));
              var packageDeclaration = newUnit.getPackageDeclaration().getReference().getQualifiedName();
              var packagePath = getPackagePath(packageDeclaration);
              var typeDeclaration = newUnit.getMainType();
            }
            for (var diff : relevantDiffs.get(FileChangeType.DELETED)) {
              var oldUnit = filesToUnits.get(pathsToLastContents.get(parentStrandId, diff.getOldPath()));
              var typeDeclaration = oldUnit.getMainType();
            }
            // TODO: Diff compilation units before and after
            // TODO: Process symbol changes
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
  }

  private static EnumMap<FileChangeType, List<DiffEntry>> selectRelevantChanges(Collection<DiffEntry> diffs) {
    var result = new EnumMap<FileChangeType, List<DiffEntry>>(FileChangeType.class);
    for (FileChangeType type : FileChangeType.values()) {
      result.put(type, new ArrayList<>());
    }
    for (var diff : diffs) {
      var changeType = diff.getChangeType();
      switch (changeType) {
        case ADD, MODIFY, COPY -> {
          if (App.isFileRelevant(diff.getNewPath())) {
            result.get(FileChangeType.fromJGitChangeType(changeType)).add(diff);
          }
        }
        case DELETE -> {
          if (App.isFileRelevant(diff.getOldPath())) {
            result.get(FileChangeType.DELETED).add(diff);
          }
        }
        case RENAME -> {
          var oldPathIsRelevant = App.isFileRelevant(diff.getOldPath());
          var newPathIsRelevant = App.isFileRelevant(diff.getNewPath());
          if (oldPathIsRelevant && newPathIsRelevant) {
            if (areSiblingPaths(diff.getOldPath(), diff.getNewPath())) {
              result.get(FileChangeType.RENAMED).add(diff);
            } else {
              result.get(FileChangeType.MOVED).add(diff);
            }
          } else if (oldPathIsRelevant) {
            result.get(FileChangeType.DELETED).add(diff);
          } else if (newPathIsRelevant) {
            result.get(FileChangeType.ADDED).add(diff);
          }
        }
      }
    }
    return result;
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

  private static boolean isFileRelevant(@NotNull String filename) {
    return filename.endsWith(".java");
  }

  private static boolean areSiblingPaths(@NotNull String path1, @NotNull String path2) {
    return Path.of(path1).getParent().equals(Path.of(path2).getParent());
  }

  private static AbstractPath getPackagePath(@NotNull String packageName) {
    return Streams.splitting(packageName, ".")
      .map(p -> "P<" + p)
      .collect(Collectors2.toPath());
  }

}
