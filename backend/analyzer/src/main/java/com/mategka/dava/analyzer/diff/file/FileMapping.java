package com.mategka.dava.analyzer.diff.file;

import com.google.common.collect.Table;
import com.mategka.dava.analyzer.extension.Pair;
import lombok.Value;

import java.util.List;

@Value
public class FileMapping {

  public record DirectFileChange(String newPath, int parentIndex, FileChange change) {}

  Table<String, Integer, FileChange> mappings;

  public List<DirectFileChange> getDirectFileChanges() {
    return mappings.rowMap().entrySet().stream()
      .map(Pair::fromEntry)
      .filter(Pair.filteringRight(m -> m.size() == 1))
      .map(Pair.mappingRight(m -> m.entrySet().iterator().next()))
      .map(Pair.mappingRight(Pair::fromEntry))
      .map(p -> new DirectFileChange(p.left(), p.right().left(), p.right().right()))
      .toList();
  }

  /*
  Types when looking at mappings:
  - If a new path is mapped for some parents -> mapping
    - Note: If a new path is unmapped as an old path at some parent and that parents maps no other old path to this new path, then this is a deletion contribution
    - If a new path is unmapped by a parent -> addition contribution
  - Paths with only addition contributions are strict additions and the DiffEntry for any parent may be used
  - Paths with only deletion contributions are strict deletions (but the DiffEntry objects must all stay intact)
  - Paths with only modification contributions are strict modifications (DiffEntry objects intact)
  - Paths with only a single contribution are strict successions

  New Path -> Index -> (ChangeType, DiffEntry)
   */

}
