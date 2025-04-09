package com.mategka.dava.analyzer.struct;

import com.mategka.dava.analyzer.extension.ListsX;
import com.mategka.dava.analyzer.git.Commit;
import com.mategka.dava.analyzer.git.Hash;
import com.mategka.dava.analyzer.struct.refactoring.SymbolRefactoring;
import com.mategka.dava.analyzer.struct.symbol.Symbol;
import com.mategka.dava.analyzer.struct.symbol.SymbolUpdate;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

@Value
@Builder
public class CommitDiff {

  /**
   * The commit SHAs of all parents compared with.
   */
  @NonNull
  List<Hash> parentCommits;

  /**
   * The SHA of the commit addressed by this diff.
   */
  @NonNull
  Hash commit;

  /**
   * The date and time of the commit addressed by this diff.
   */
  @NonNull
  ZonedDateTime commitDate;

  @NonNull
  Collection<SymbolRefactoring> refactorings;

  @NonNull
  Collection<Symbol> additions;

  @NonNull
  Collection<Symbol> deletions;

  @NonNull
  Collection<SymbolUpdate> updates;

  public static class CommitDiffBuilder {

    public CommitDiffBuilder commitData(Commit commit) {
      return parentCommits(ListsX.map(commit.parents(), Commit::hash))
        .commit(commit.hash())
        .commitDate(commit.dateTime());
    }

  }

}
