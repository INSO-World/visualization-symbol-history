package com.mategka.dava.analyzer.git;

import com.mategka.dava.analyzer.extension.PathsX;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.experimental.UtilityClass;
import org.eclipse.jgit.diff.DiffEntry;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

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

  private boolean isFileRelevant(@NotNull String filename) {
    return filename.endsWith(".java");
  }

}
