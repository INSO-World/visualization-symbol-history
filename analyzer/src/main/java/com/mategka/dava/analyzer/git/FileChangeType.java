package com.mategka.dava.analyzer.git;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;

import static org.eclipse.jgit.diff.DiffEntry.ChangeType;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(doNotUseGetters = true)
public enum FileChangeType {

  ADDED(ChangeType.ADD, false, true),
  DELETED(ChangeType.DELETE, true, false),
  MODIFIED(ChangeType.MODIFY, false, true),
  MOVED(ChangeType.RENAME, true, true),
  RENAMED(ChangeType.RENAME, true, true),
  COPIED(ChangeType.COPY, false, true),
  ;

  ChangeType jGitChangeType;
  boolean removingOldResource;
  boolean addingNewResource;

  public static FileChangeType fromJGitChangeType(ChangeType changeType) {
    return Arrays.stream(values())
      .filter(type -> type.jGitChangeType == changeType)
      .findFirst()
      .orElseThrow();
  }

}
