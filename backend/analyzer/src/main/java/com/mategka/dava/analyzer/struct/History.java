package com.mategka.dava.analyzer.struct;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.mategka.dava.analyzer.git.Commits;
import com.mategka.dava.analyzer.struct.symbol.Symbol;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
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
  Set<Branch> baseBranches;

  @NonNull
  @SuppressWarnings("UnstableApiUsage")
  Graph<Branch> branchDag;

  @NonNull
  Map<String, Branch> branchMapping;

  // Symbols are associated with a running ID
  // Property Update = Sealed hierarchy of types with unique name, new value and commit SHA
  // Initial commit / Addition = new ID key + property updates for all mandatory and present optional properties
  // Deletion = ID key of removed symbol + removal commit SHA
  // Update = ID key of symbol + property updates, commit SHA
  // Refactoring = Sealed hierarchy of types with input symbol ID keys, output symbol ID keys and commit SHA
  //               (including output symbol property updates IF output symbol is NEW)
  // During scan: commits have branch ID based on concurrent traversal path counts
  //              keep track of all latest symbol states for all branch IDs
  // On merge commits: assume changes from incoming branch are "incorporated" (n-way merge necessary?)
  // In final result: include final state of all then-present and removed symbols on primary branch

  @Deprecated
  @SuppressWarnings("UnstableApiUsage")
  public static History emptyOfBranchSinglePass(@NotNull Repository repository, Ref head) throws IOException {
    Set<Branch> baseBranches = new HashSet<>();
    MutableGraph<Branch> branchDag = GraphBuilder.directed().allowsSelfLoops(false).build();
    Map<String, Branch> branchMapping = new HashMap<>();
    try (RevWalk revWalk = Commits.topological(repository, head)) {
      Set<String> separatedParents = new HashSet<>();
      var initialBranch = Branch.builder().id(0).name(head.getName()).build();
      baseBranches.add(initialBranch);
      branchDag.addNode(initialBranch);
      boolean first = true;
      for (RevCommit commit : revWalk) {
        var sha = commit.getId().getName();
        if (first) {
          branchMapping.put(sha, initialBranch);
          first = false;
        }
        var assignedBranch = branchMapping.get(sha);
        var parentShasByMappingExistence = Arrays.stream(commit.getParents())
          .map(RevObject::getId)
          .map(AnyObjectId::getName)
          .collect(Collectors.partitioningBy(branchMapping::containsKey));
        var encounteredParentShas = parentShasByMappingExistence.get(true);
        var newParentShas = parentShasByMappingExistence.get(false);
        if (!encounteredParentShas.isEmpty() || newParentShas.size() > 1) {
          baseBranches.remove(assignedBranch);
        }
        for (var parentSha : encounteredParentShas) {
          var mappedBranch = branchMapping.get(parentSha);
          if (separatedParents.contains(parentSha)) {
            branchDag.putEdge(mappedBranch, assignedBranch);
          } else {
            baseBranches.remove(mappedBranch);
            var parentBranch = Branch.builder().id(branchDag.nodes().size()).name(parentSha).build();
            branchDag.addNode(parentBranch);
            branchDag.putEdge(parentBranch, assignedBranch);
            branchDag.putEdge(parentBranch, mappedBranch);
            baseBranches.add(parentBranch);
            branchMapping.put(parentSha, parentBranch);
            separatedParents.add(parentSha);
          }
        }
        if (newParentShas.isEmpty()) {
          continue;
        }
        var parentCount = encounteredParentShas.size() + newParentShas.size();
        if (parentCount == 1) {
          var parentSha = newParentShas.getFirst();
          branchMapping.put(parentSha, assignedBranch);
        } else {
          for (String parentSha : newParentShas) {
            var parentBranch = Branch.builder().id(branchDag.nodes().size()).name(commit.getShortMessage()).build();
            branchDag.addNode(parentBranch);
            branchDag.putEdge(parentBranch, assignedBranch);
            baseBranches.add(parentBranch);
            branchMapping.put(parentSha, parentBranch);
            separatedParents.add(parentSha);
          }
        }
        commit.disposeBody();
      }
      System.out.println(
        branchDag.edges().stream()
          .map(e -> "%d %d".formatted(e.nodeU().getId(), e.nodeV().getId()))
          .collect(Collectors.joining("\n"))
      );
      System.out.println(
        branchDag.nodes().stream()
          .map(b -> "%d %s".formatted(b.getId(), b.getName()))
          .collect(Collectors.joining("\n"))
      );
    }
    return History.builder()
      .baseBranches(baseBranches)
      .branchDag(branchDag)
      .branchMapping(branchMapping)
      .build();
  }

  @SuppressWarnings("UnstableApiUsage")
  public static History emptyOfBranch(@NotNull Repository repository, Ref head) throws IOException {
    Set<Branch> baseBranches = new HashSet<>();
    MutableGraph<Branch> branchDag = GraphBuilder.directed().allowsSelfLoops(false).build();
    Map<String, Branch> branchMapping = new HashMap<>();
    Map<String, Boolean> multiChildCommitsMap = new HashMap<>();
    try (RevWalk revWalk = Commits.topological(repository, head)) {
      for (RevCommit commit : revWalk) {
        commit.disposeBody();
        var parentShas = Arrays.stream(commit.getParents())
          .map(RevObject::getId)
          .map(AnyObjectId::getName)
          .toList();
        for (var parentSha : parentShas) {
          multiChildCommitsMap.compute(parentSha, (k, v) -> v != null);
        }
      }
    }
    Set<String> multiChildCommits = multiChildCommitsMap.entrySet().stream()
      .filter(Map.Entry::getValue)
      .map(Map.Entry::getKey)
      .collect(Collectors.toSet());
    try (RevWalk revWalk = Commits.topologicalReverse(repository, head)) {
      for (RevCommit commit : revWalk) {
        String commitMessage = commit.getShortMessage();
        var sha = commit.getId().getName();
        commit.disposeBody();
        var parentShas = Arrays.stream(commit.getParents())
          .map(RevObject::getId)
          .map(AnyObjectId::getName)
          .toList();
        var hasMultiChildParent = parentShas.stream().anyMatch(multiChildCommits::contains);
        var parentBranches = parentShas.stream().map(branchMapping::get).toList();
        if (hasMultiChildParent || parentShas.size() != 1) {
          var branch = Branch.builder().id(branchDag.nodes().size()).name(commitMessage).build();
          branchDag.addNode(branch);
          branchMapping.put(sha, branch);
          if (parentBranches.isEmpty()) {
            baseBranches.add(branch);
          } else {
            parentBranches.forEach(parentBranch -> branchDag.putEdge(parentBranch, branch));
          }
        } else {
          branchMapping.put(sha, parentBranches.getFirst());
        }
      }
      System.out.println(
        branchDag.edges().stream()
          .map(e -> "%d %d".formatted(e.nodeU().getId(), e.nodeV().getId()))
          .collect(Collectors.joining("\n"))
      );
      System.out.println(
        branchDag.nodes().stream()
          .map(b -> "%d %s".formatted(b.getId(), b.getName()))
          .collect(Collectors.joining("\n"))
      );
    }
    return History.builder()
      .baseBranches(baseBranches)
      .branchDag(branchDag)
      .branchMapping(branchMapping)
      .build();
  }

}
