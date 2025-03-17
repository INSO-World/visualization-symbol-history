package com.mategka.dava.analyzer.diff.file;

import com.mategka.dava.analyzer.collections.MultisetArray;
import com.mategka.dava.analyzer.git.FileChangeType;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FileDiff {

  public FileMapping getMapping(MultisetArray<FileChange> diffMap) {
    // TODO: Fix unchanged files not being detected anywhere (problematic for merge commits)
    Table<String, Integer, FileChange> mapping = HashBasedTable.create(32, diffMap.length());
    for (var diffEntry : diffMap.asMap().entrySet()) {
      var parentIndex = diffEntry.getKey();
      var changes = diffEntry.getValue();
      for (FileChange change : changes) {
        var path = (change.changeType() == FileChangeType.DELETED) ? change.getOldPath() : change.getNewPath();
        mapping.put(path, parentIndex, change);
      }
    }
    return new FileMapping(mapping);
  }

}
