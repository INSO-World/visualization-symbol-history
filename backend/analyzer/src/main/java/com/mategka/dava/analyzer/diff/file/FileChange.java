package com.mategka.dava.analyzer.diff.file;

import com.mategka.dava.analyzer.git.FileChangeType;

import org.eclipse.jgit.diff.DiffEntry;

public record FileChange(FileChangeType changeType, DiffEntry diffEntry) {

  public String getOldPath() {
    return changeType == FileChangeType.ADDED ? null : diffEntry.getOldPath();
  }

  public String getNewPath() {
    return changeType == FileChangeType.DELETED ? null : diffEntry.getNewPath();
  }

}
