package com.mategka.dava.analyzer.struct;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class Branch {

  long id;

  @NonNull
  String name;

  @NonNull
  @Builder.Default
  List<CommitDiff> commitDiffs = new ArrayList<>();

}
