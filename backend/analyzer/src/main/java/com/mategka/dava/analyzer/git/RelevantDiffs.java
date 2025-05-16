package com.mategka.dava.analyzer.git;

import com.mategka.dava.analyzer.diff.file.FileChange;
import com.mategka.dava.analyzer.extension.PathsX;
import com.mategka.dava.analyzer.extension.struct.Pair;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.experimental.UtilityClass;
import org.eclipse.jgit.diff.DiffEntry;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

@UtilityClass
public class RelevantDiffs {

  public Multimap<FileChangeType, DiffEntry> extract(Collection<DiffEntry> diffs) {
    var result = HashMultimap.<FileChangeType, DiffEntry>create();
    for (var diff : diffs) {
      var changeType = diff.getChangeType();
      switch (changeType) {
        case ADD, MODIFY, COPY -> {
          if (isFileRelevant(diff.getNewPath())) {
            result.put(FileChangeType.fromJGitChangeType(changeType), diff);
          }
        }
        case DELETE -> {
          if (isFileRelevant(diff.getOldPath())) {
            result.put(FileChangeType.DELETED, diff);
          }
        }
        case RENAME -> {
          var oldPathIsRelevant = isFileRelevant(diff.getOldPath());
          var newPathIsRelevant = isFileRelevant(diff.getNewPath());
          if (oldPathIsRelevant && newPathIsRelevant) {
            if (PathsX.areSiblingPaths(diff.getOldPath(), diff.getNewPath())) {
              result.put(FileChangeType.RENAMED, diff);
            } else {
              result.put(FileChangeType.MOVED, diff);
            }
          } else if (oldPathIsRelevant) {
            result.put(FileChangeType.DELETED, diff);
          } else if (newPathIsRelevant) {
            result.put(FileChangeType.ADDED, diff);
          }
        }
      }
    }
    return result;
  }

  public List<FileChange> extract2(Collection<DiffEntry> diffs) {
    return diffs.stream()
      .filter(RelevantDiffs::isDiffRelevant)
      .map(Pair.fromRight(RelevantDiffs::getChangeType))
      .map(Pair.folding(FileChange::new))
      .toList();
  }

  public FileChangeType getChangeType(@NotNull DiffEntry diff) {
    var changeType = diff.getChangeType();
    return switch (changeType) {
      case ADD, MODIFY, DELETE -> FileChangeType.fromJGitChangeType(changeType);
      // TODO: Treat copied files as copies instead of additions
      case COPY -> FileChangeType.ADDED;
      case RENAME -> {
        var oldPathIsRelevant = isFileRelevant(diff.getOldPath());
        if (!oldPathIsRelevant) {
          yield FileChangeType.ADDED;
        }
        var newPathIsRelevant = isFileRelevant(diff.getNewPath());
        if (!newPathIsRelevant) {
          yield FileChangeType.DELETED;
        }
        if (PathsX.areSiblingPaths(diff.getOldPath(), diff.getNewPath())) {
          yield FileChangeType.RENAMED;
        } else {
          yield FileChangeType.MOVED;
        }
      }
    };
  }

  public boolean isDiffRelevant(@NotNull DiffEntry diff) {
    var changeType = diff.getChangeType();
    return switch (changeType) {
      case ADD, MODIFY, COPY -> isFileRelevant(diff.getNewPath());
      case DELETE -> isFileRelevant(diff.getOldPath());
      case RENAME -> {
        var oldPathIsRelevant = isFileRelevant(diff.getOldPath());
        var newPathIsRelevant = isFileRelevant(diff.getNewPath());
        yield oldPathIsRelevant || newPathIsRelevant;
      }
    };
  }

  public boolean isFileRelevant(@NotNull String filename) {
    return filename.endsWith(".java");
  }

}
