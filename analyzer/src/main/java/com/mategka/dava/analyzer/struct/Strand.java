package com.mategka.dava.analyzer.struct;

import com.mategka.dava.analyzer.extension.Mutable;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@ToString
@Builder
public final class Strand {

  long id;

  @NonNull
  String name;

  @NonNull
  @Builder.Default
  @Mutable
  List<CommitDiff> commitDiffs = new ArrayList<>();

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Strand strand)) return false;
    return id == strand.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

}
