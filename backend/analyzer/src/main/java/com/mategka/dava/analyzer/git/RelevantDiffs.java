package com.mategka.dava.analyzer.git;

import com.mategka.dava.analyzer.extension.Paths2;

import lombok.experimental.UtilityClass;
import org.eclipse.jgit.diff.DiffEntry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;

@UtilityClass
public class RelevantDiffs {

  public EnumMap<FileChangeType, List<DiffEntry>> extract(Collection<DiffEntry> diffs) {
    var result = new EnumMap<FileChangeType, List<DiffEntry>>(FileChangeType.class);
    for (FileChangeType type : FileChangeType.values()) {
      result.put(type, new ArrayList<>());
    }
    for (var diff : diffs) {
      var changeType = diff.getChangeType();
      switch (changeType) {
        case ADD, MODIFY, COPY -> {
          if (isFileRelevant(diff.getNewPath())) {
            result.get(FileChangeType.fromJGitChangeType(changeType)).add(diff);
          }
        }
        case DELETE -> {
          if (isFileRelevant(diff.getOldPath())) {
            result.get(FileChangeType.DELETED).add(diff);
          }
        }
        case RENAME -> {
          var oldPathIsRelevant = isFileRelevant(diff.getOldPath());
          var newPathIsRelevant = isFileRelevant(diff.getNewPath());
          if (oldPathIsRelevant && newPathIsRelevant) {
            if (Paths2.areSiblingPaths(diff.getOldPath(), diff.getNewPath())) {
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

  private boolean isFileRelevant(@NotNull String filename) {
    return filename.endsWith(".java");
  }

}
