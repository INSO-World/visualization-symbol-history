package com.mategka.dava.analyzer.struct;

import com.mategka.dava.analyzer.extension.ListsX;
import com.mategka.dava.analyzer.git.Commit;
import com.mategka.dava.analyzer.git.Hash;
import com.mategka.dava.analyzer.struct.property.LineRangeProperty;
import com.mategka.dava.analyzer.struct.property.index.PropertyKeys;
import com.mategka.dava.analyzer.struct.refactoring.SymbolRefactoring;
import com.mategka.dava.analyzer.struct.symbol.Symbol;
import com.mategka.dava.analyzer.struct.symbol.SymbolUpdate;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

  public void printDebug() {
    additions.stream()
      .sorted(Comparator.comparingLong(s -> s.getKey().symbolId()))
      .forEach(a -> System.out.println("+ " + a));
    deletions.stream()
      .sorted(Comparator.comparingLong(s -> s.getKey().symbolId()))
      .forEach(d -> System.out.printf("- @%d %s%n", d.getContext().getOrThrow().key().strandId(), d));
    var updatesByJustLineChanges = updates.stream()
      .sorted(Comparator.comparingLong(u -> u.getSourceKey().symbolId()))
      .collect(Collectors.partitioningBy(u -> u.getProperties().size() == 1
        && u.getProperties().values().iterator().next().getKey().equals(PropertyKeys.get(LineRangeProperty.class))));
    var lineUpdates = updatesByJustLineChanges.get(true).stream()
      .map(u -> u.getSourceKey().symbolId())
      .map(Objects::toString)
      .collect(Collectors.joining(", "));
    if (!lineUpdates.isEmpty()) {
      System.out.println("$ Line changes: " + lineUpdates);
    }
    updatesByJustLineChanges.get(false).stream()
      .sorted(Comparator.comparingLong(u -> u.getSourceKey().symbolId()))
      .forEach(u -> System.out.println("$ " + u));
  }

  public static class CommitDiffBuilder {

    public CommitDiffBuilder commitData(Commit commit) {
      return parentCommits(ListsX.map(commit.parents(), Commit::hash))
        .commit(commit.hash())
        .commitDate(commit.dateTime());
    }

  }

}
