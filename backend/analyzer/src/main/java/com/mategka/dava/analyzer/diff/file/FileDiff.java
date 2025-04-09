package com.mategka.dava.analyzer.diff.file;

import com.mategka.dava.analyzer.collections.Array;
import com.mategka.dava.analyzer.collections.ManyToManyMap;

import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public class FileDiff {

  public FileMapping getMapping(Array<Map<String, FileChange>> diffsPerParent,
                                Array<Map<String, FileChange>> additionsPerParent) {
    var mapping = new ManyToManyMap<ParentFile, String, FileChange>();
    for (var parentEntry : diffsPerParent.withIndex()) {
      int parentIndex = parentEntry.left();
      var diffs = parentEntry.right();
      for (var sourcePath : diffs.keySet()) {
        var fileChange = diffs.get(sourcePath);
        if (fileChange.getNewPath() == null) {
          // Ignore mappings for deletions
          continue;
        }
        var parentFile = new ParentFile(parentIndex, sourcePath);
        mapping.put(parentFile, fileChange.getNewPath(), fileChange);
      }
    }
    for (var parentEntry : additionsPerParent.withIndex()) {
      int parentIndex = parentEntry.left();
      var diffs = parentEntry.right();
      for (var targetPath : diffs.keySet()) {
        var fileChange = diffs.get(targetPath);
        var parentFile = new ParentFile(parentIndex, null);
        mapping.put(parentFile, targetPath, fileChange);
      }
    }
    return new FileMapping(mapping);
  }

}
