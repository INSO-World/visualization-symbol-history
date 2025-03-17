package com.mategka.dava.analyzer.util.progress;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public sealed class CompoundTask implements Task permits MainTask {

  int size;
  long subtaskCount;

}
