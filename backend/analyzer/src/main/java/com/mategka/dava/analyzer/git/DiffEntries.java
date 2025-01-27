package com.mategka.dava.analyzer.git;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.*;

public class DiffEntries extends DiffEntry {

  private static final AbbreviatedObjectId A_ZERO = AbbreviatedObjectId
    .fromObjectId(ObjectId.zeroId());

  public static DiffEntry newAddition(String path, AnyObjectId id) {
    return new AddEntry(path, id);
  }

  private static class AddEntry extends DiffEntry {

    private AddEntry(String path, AnyObjectId id) {
      oldId = A_ZERO;
      oldMode = FileMode.MISSING;
      oldPath = DEV_NULL;

      newId = AbbreviatedObjectId.fromObjectId(id);
      newMode = FileMode.REGULAR_FILE;
      newPath = path;
      changeType = ChangeType.ADD;
    }

  }

}
