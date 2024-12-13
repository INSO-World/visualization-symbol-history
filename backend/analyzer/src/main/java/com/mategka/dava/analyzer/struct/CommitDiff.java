package com.mategka.dava.analyzer.struct;

import com.google.common.collect.ListMultimap;
import com.mategka.dava.analyzer.struct.refactoring.SymbolRefactoring;
import com.mategka.dava.analyzer.struct.symbol.Symbol;
import lombok.NonNull;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
public class CommitDiff {

  @NonNull
  List<CommitSha> parentCommits;

  @NonNull
  CommitSha commit;

  @NonNull
  ZonedDateTime commitDate;

  @NonNull
  List<SymbolRefactoring> refactorings;

  @NonNull
  List<Symbol> additions;

  @NonNull
  List<Long> deletions;

  @NonNull
  ListMultimap<Long, SymbolUpdate> updates;

}
