package com.mategka.dava.analyzer.struct;

import com.mategka.dava.analyzer.extension.IterablesX;
import com.mategka.dava.analyzer.extension.ListsX;
import com.mategka.dava.analyzer.git.Commit;
import com.mategka.dava.analyzer.git.Hash;
import com.mategka.dava.analyzer.struct.property.LineRangeProperty;
import com.mategka.dava.analyzer.struct.property.index.PropertyKeys;
import com.mategka.dava.analyzer.struct.symbol.Symbol;
import com.mategka.dava.analyzer.struct.symbol.SymbolUpdate;

import com.google.common.collect.Iterables;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Value
@Builder
public class CommitDiff implements Serializable {

  @Serial
  private static final long serialVersionUID = 4511490283001393772L;

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
  Collection<Symbol> additions;

  @NonNull
  Collection<Symbol> successions;

  @NonNull
  Collection<Symbol> deletions;

  @NonNull
  Collection<SymbolUpdate> updates;

  public Iterable<Symbol> getStartSymbols() {
    return Iterables.concat(additions, successions);
  }

  public void printDebug() {
    additions.stream()
      .sorted(Comparator.comparingLong(s -> s.getKey().symbolId()))
      .forEach(a -> System.out.println("+ " + a));
    deletions.stream()
      .sorted(Comparator.comparingLong(s -> s.getKey().symbolId()))
      .forEach(d -> System.out.printf("- @%d %s%n", d.getContext().getOrThrow().key().strandId(), d));
    var updatesByJustLineChanges = updates.stream()
      .sorted(Comparator.comparingLong(u -> u.getSourceContext().key().symbolId()))
      .collect(Collectors.partitioningBy(u -> u.getProperties().size() == 1
        && IterablesX.getFirst(u.getProperties().keySet()).equals(PropertyKeys.get(LineRangeProperty.class))));
    var lineUpdates = updatesByJustLineChanges.get(true).stream()
      .map(u -> u.getSourceContext().key().symbolId())
      .map(Objects::toString)
      .collect(Collectors.joining(", "));
    if (!lineUpdates.isEmpty()) {
      System.out.println("$ Line changes: " + lineUpdates);
    }
    updatesByJustLineChanges.get(false).stream()
      .sorted(Comparator.comparingLong(u -> u.getSourceContext().key().symbolId()))
      .forEach(u -> System.out.println("$ " + u));
  }

  public long size() {
    return (long) additions.size() + successions.size() + deletions.size() + updates.size();
  }

  public static class CommitDiffBuilder {

    public CommitDiffBuilder commitData(Commit commit) {
      return parentCommits(ListsX.map(commit.parents(), Commit::hash))
        .commit(commit.hash())
        .commitDate(commit.dateTime());
    }

  }

}
