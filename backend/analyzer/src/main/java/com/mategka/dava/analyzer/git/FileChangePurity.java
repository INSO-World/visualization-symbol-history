package com.mategka.dava.analyzer.git;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(doNotUseGetters = true)
public enum FileChangePurity {

  ONLY_ADDITIONS,
  ONLY_DELETIONS,
  MIXED_CHANGES,

}
