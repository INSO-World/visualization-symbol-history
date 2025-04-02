package com.mategka.dava.analyzer.struct;

import com.mategka.dava.analyzer.extension.ListsX;
import com.mategka.dava.analyzer.git.Commit;
import com.mategka.dava.analyzer.git.Hash;
import com.mategka.dava.analyzer.struct.refactoring.SymbolRefactoring;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

  /**
   * A map from symbol IDs to trace symbols introduced by this commit.
   * Logically, this map will always be empty for commits not first in their
   * corresponding strand.
   * These can be thought of as additions without any actual symbol to add and
   * should always be processed by consumers first.
   * Note that trace symbols may be deleted in the commit they were created,
   * in which case they show up both here and in {@link #getDeletions()}.
   * Refactorings and modifications will likewise be reflected by
   * {@link #getRefactorings()} and {@link #getUpdates()}.
   */
  @NonNull
  Map<Long, Symbol> successions;

  /**
   * A list of all multi-symbol refactorings performed in this commit.
   * Single-symbol refactorings such as simple moves and renames can instead be
   * found in {@link #getUpdates()}.
   */
  @NonNull
  List<SymbolRefactoring> refactorings;

  /**
   * A list of all symbols introduced in this commit.
   * Their state mirrors the properties they were introduced with.
   * Symbols introduced by multi-symbol refactorings do not show up here.
   *
   * @see #getSuccessions()
   * @see #getRefactorings()
   */
  @NonNull
  List<Symbol> additions;

  /**
   * A list of all symbols removed in this commit.
   * Their state mirrors the properties they had prior to deletion.
   * Symbols removed by multi-symbol refactorings do not show up here.
   *
   * @see #getRefactorings()
   */
  @NonNull
  List<Symbol> deletions;

  /**
   * A list of all symbols modified in this commit.
   * Symbols modified by multi-symbol refactorings do not show up here.
   *
   * @see #getRefactorings()
   */
  @NonNull
  Map<Long, SymbolUpdate> updates;

  public static CommitDiff empty(Commit commit) {
    return CommitDiff.builder()
      .commitData(commit)
      .successions(Collections.emptyMap())
      .refactorings(Collections.emptyList())
      .additions(Collections.emptyList())
      .deletions(Collections.emptyList())
      .updates(Collections.emptyMap())
      .build();
  }

  public static class CommitDiffBuilder {

    public CommitDiffBuilder commitData(Commit commit) {
      return parentCommits(ListsX.map(commit.parents(), Commit::hash))
        .commit(commit.hash())
        .commitDate(commit.dateTime());
    }

  }

}
