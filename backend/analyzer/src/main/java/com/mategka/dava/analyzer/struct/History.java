package com.mategka.dava.analyzer.struct;

import com.google.common.graph.*;
import com.mategka.dava.analyzer.git.CommitOrder;
import com.mategka.dava.analyzer.git.RepositoryWrapper;
import com.mategka.dava.analyzer.struct.symbol.Symbol;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevWalk;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Value
@Builder(access = AccessLevel.PRIVATE)
public class History {

  @NonNull
  @Builder.Default
  List<Symbol> presentSymbols = new ArrayList<>();

  @NonNull
  @Builder.Default
  List<Symbol> deletedSymbols = new ArrayList<>();

  @NonNull
  Set<Strand> baseStrands;

  @NonNull
  @SuppressWarnings("UnstableApiUsage")
  Graph<Strand> strandDag;

  @NonNull
  Map<String, Strand> strandMapping;

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

  @SuppressWarnings("UnstableApiUsage")
  public static History emptyOfBranch(@NotNull RepositoryWrapper repository, Ref head) throws IOException {
    Set<Strand> baseStrands = new HashSet<>();
    MutableGraph<Strand> strandDag = GraphBuilder.directed().allowsSelfLoops(false).build();
    Map<String, Strand> strandMapping = new HashMap<>();
    Set<String> parentCommits = new HashSet<>();
    Set<String> multiChildCommits = new HashSet<>();
    try (RevWalk revWalk = repository.commitsUpTo(head, CommitOrder.TOPOLOGICAL)) {
      for (RevCommit commit : revWalk) {
        commit.disposeBody();
        var parentShas = Arrays.stream(commit.getParents())
          .map(RevObject::getId)
          .map(AnyObjectId::getName)
          .toList();
        for (var parentSha : parentShas) {
          if (parentCommits.contains(parentSha)) {
            multiChildCommits.add(parentSha);
          } else {
            parentCommits.add(parentSha);
          }
        }
      }
    }
    try (RevWalk revWalk = repository.commitsUpTo(head, CommitOrder.REVERSE_TOPOLOGICAL)) {
      for (RevCommit commit : revWalk) {
        String commitMessage = commit.getShortMessage();
        var sha = commit.getId().getName();
        commit.disposeBody();
        var parentShas = Arrays.stream(commit.getParents())
          .map(RevObject::getId)
          .map(AnyObjectId::getName)
          .toList();
        var hasMultiChildParent = parentShas.stream().anyMatch(multiChildCommits::contains);
        var parentStrands = parentShas.stream().map(strandMapping::get).toList();
        if (hasMultiChildParent || parentShas.size() != 1) {
          var strand = Strand.builder().id(strandDag.nodes().size()).name(commitMessage).build();
          strandDag.addNode(strand);
          strandMapping.put(sha, strand);
          if (parentStrands.isEmpty()) {
            baseStrands.add(strand);
          } else {
            parentStrands.forEach(parentStrand -> strandDag.putEdge(parentStrand, strand));
          }
        } else {
          strandMapping.put(sha, parentStrands.getFirst());
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
      .baseStrands(baseStrands)
      .strandDag(strandDag)
      .strandMapping(strandMapping)
      .build();
  }

}
