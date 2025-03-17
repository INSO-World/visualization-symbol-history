package com.mategka.dava.analyzer.diff.file;

import com.mategka.dava.analyzer.git.FileChangeType;

import org.eclipse.jgit.diff.DiffEntry;

public record FileChange(FileChangeType changeType, DiffEntry diffEntry) {

  public String getOldPath() {
    return diffEntry().getOldPath();
  }

  public String getNewPath() {
    return diffEntry().getNewPath();
  }

}
