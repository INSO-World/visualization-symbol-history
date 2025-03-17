package com.mategka.dava.analyzer.diff.file;

import com.mategka.dava.analyzer.extension.PathsX;
import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.git.Commit;
import com.mategka.dava.analyzer.git.FileChangeType;
import com.mategka.dava.analyzer.git.RepositoryWrapper;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import lombok.experimental.UtilityClass;
import org.eclipse.jgit.diff.DiffEntry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.mategka.dava.analyzer.extension.option.Option.None;
import static com.mategka.dava.analyzer.extension.option.Option.Some;

@UtilityClass
public class FileDiff {

  public FileMapping getMapping(RepositoryWrapper repository, Commit commit) {
    var parents = commit.parents();
    Table<String, Integer, FileChange> mapping = HashBasedTable.create(32, parents.size());
    for (int parentIndex = 0; parentIndex < parents.size(); parentIndex++) {
      var parent = parents.get(parentIndex);
      try (var formatter = repository.newFormatter()) {
        var diffs = formatter.scan(parent.tree(), commit.tree());
        var relevantChanges = AnStream.from(diffs)
          .mapOption(FileDiff::yieldIfRelevant)
          .toList();
        for (var change : relevantChanges) {
          var path = (change.changeType() == FileChangeType.DELETED) ? change.getOldPath() : change.getNewPath();
          mapping.put(path, parentIndex, change);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return new FileMapping(mapping);
  }

  private boolean isFileRelevant(@NotNull String filename) {
    return filename.endsWith(".java");
  }

  private Option<FileChange> yieldIfRelevant(@NotNull DiffEntry diff) {
    var changeType = diff.getChangeType();
    switch (changeType) {
      case ADD, MODIFY, COPY -> {
        if (isFileRelevant(diff.getNewPath())) {
          return Some(new FileChange(FileChangeType.fromJGitChangeType(changeType), diff));
        }
      }
      case DELETE -> {
        if (isFileRelevant(diff.getOldPath())) {
          return Some(new FileChange(FileChangeType.DELETED, diff));
        }
      }
      case RENAME -> {
        var oldPathIsRelevant = isFileRelevant(diff.getOldPath());
        var newPathIsRelevant = isFileRelevant(diff.getNewPath());
        if (oldPathIsRelevant && newPathIsRelevant) {
          if (PathsX.areSiblingPaths(diff.getOldPath(), diff.getNewPath())) {
            return Some(new FileChange(FileChangeType.RENAMED, diff));
          } else {
            return Some(new FileChange(FileChangeType.MOVED, diff));
          }
        } else if (oldPathIsRelevant) {
          return Some(new FileChange(FileChangeType.DELETED, diff));
        } else if (newPathIsRelevant) {
          return Some(new FileChange(FileChangeType.ADDED, diff));
        }
      }
    }
    return None();
  }

}
