package com.mategka.dava.analyzer.struct;

import com.mategka.dava.analyzer.collections.DAG;
import com.mategka.dava.analyzer.extension.ListsX;
import com.mategka.dava.analyzer.git.*;
import com.mategka.dava.analyzer.struct.symbol.Symbol;

import lombok.*;
import org.eclipse.jgit.lib.ObjectId;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Value
@Builder(access = AccessLevel.PRIVATE)
public class History {

  @NonNull
  String name;

  /**
   * Contains all symbols present at the HEAD commit with their final properties.
   *
   * @deprecated Will not be populated.
   */
  @NonNull
  @Builder.Default
  @Deprecated
  List<Symbol> presentSymbols = Collections.emptyList();

  /**
   * Contains all symbols that were deleted at some point with their final properties.
   * Note that these may contain trace symbols with identical original symbols.
   *
   * @deprecated Will not be populated.
   */
  @NonNull
  @Builder.Default
  @Deprecated
  List<Symbol> deletedSymbols = Collections.emptyList();

  /**
   * Contains all strands with initial commits (source nodes in the {@linkplain #getStrandDag() strand DAG}).
   */
  @NonNull
  Set<Strand> baseStrands;

  @NonNull
  DAG<Strand> strandDag;

  @NonNull
  Map<Hash, Strand> strandMapping;

  // Symbols are associated with a running ID
  // Property Update = Sealed hierarchy of types with unique name, new value and commit SHA
  // Initial commit / Addition = new ID key + property updates for all mandatory and present optional properties
  // Deletion = ID key of removed symbol + removal commit SHA
  // Update = ID key of symbol + property updates, commit SHA
  // Refactoring = Sealed hierarchy of types with input symbol ID keys, output symbol ID keys and commit SHA
  //               (including output symbol property updates IF output symbol is NEW)
  // During scan: commits have branch ID based on concurrent traversal path counts
  //              keep track of all latest symbol states for all branch IDs
  // On merge commits: assume changes from incoming branch are "incorporated" (n-way merge necessary)
  // In final result: include final state of all then-present and removed symbols on branch of HEAD
  int commitCount;

  @SuppressWarnings("UnstableApiUsage")
  public static History emptyOfBranch(@NotNull Repository repository, ObjectId head) throws IOException {
    int commitCount = 0;
    Set<Strand> baseStrands = new HashSet<>();
    var strandDag = new DAG<Strand>();
    Map<Hash, Strand> strandMapping = new HashMap<>();
    Set<Hash> parentCommits = new HashSet<>();
    Set<Hash> multiChildCommits = new HashSet<>();
    try (CommitWalk commitWalk = repository.commitsUpTo(head, CommitOrder.TOPOLOGICAL)) {
      for (Commit commit : commitWalk) {
        commit.disposeBody();
        commitCount++;
        commit.parents().stream()
          .map(Commit::hash)
          .forEach((parentHash) -> {
            if (parentCommits.contains(parentHash)) {
              multiChildCommits.add(parentHash);
            } else {
              parentCommits.add(parentHash);
            }
          });
      }
    }
    try (CommitWalk commitWalk = repository.commitsUpTo(head, CommitOrder.REVERSE_TOPOLOGICAL)) {
      for (Commit commit : commitWalk) {
        String commitMessage = commit.summary();
        var hash = commit.hash();
        commit.disposeBody();
        var parentHashes = ListsX.map(commit.parents(), Commit::hash);
        var hasMultiChildParent = parentHashes.stream().anyMatch(multiChildCommits::contains);
        var parentStrands = ListsX.map(parentHashes, strandMapping::get);
        if (hasMultiChildParent || parentHashes.size() != 1) {
          var strand = Strand.builder()
            .id(strandDag.nodes().size())
            .name(commitMessage)
            .build();
          strandDag.addNode(strand);
          strandMapping.put(hash, strand);
          if (parentStrands.isEmpty()) {
            baseStrands.add(strand);
          } else {
            parentStrands.forEach(parentStrand -> strandDag.putEdge(parentStrand, strand));
          }
        } else {
          strandMapping.put(hash, parentStrands.getFirst());
        }
      }
    }
    System.out.println(
      strandDag.edges().stream()
        .map(e -> "%d %d".formatted(e.nodeU().getId(), e.nodeV().getId()))
        .collect(Collectors.joining("\n"))
    );
    System.out.println(
      strandDag.nodes().stream()
        .map(b -> "%s -> %d -> %s".formatted(
          strandDag.predecessors(b).stream().map(Strand::getId).toList(),
          b.getId(),
          strandDag.successors(b).stream().map(Strand::getId).toList()
        ))
        .collect(Collectors.joining("\n"))
    );
    System.out.println(
      strandDag.nodes().stream()
        .map(b -> "%d %s".formatted(b.getId(), b.getName()))
        .collect(Collectors.joining("\n"))
    );
    return History.builder()
      .name(repository.getName())
      .baseStrands(baseStrands)
      .strandDag(strandDag)
      .strandMapping(strandMapping)
      .commitCount(commitCount)
      .build();
  }

}
