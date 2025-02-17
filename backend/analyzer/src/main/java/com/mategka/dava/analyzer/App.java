package com.mategka.dava.analyzer;

import com.mategka.dava.analyzer.collections.ChainMap;
import com.mategka.dava.analyzer.collections.DefaultMap;
import com.mategka.dava.analyzer.collections.IndexMap;
import com.mategka.dava.analyzer.extension.*;
import com.mategka.dava.analyzer.git.*;
import com.mategka.dava.analyzer.spoon.AstComparator;
import com.mategka.dava.analyzer.spoon.Spoon;
import com.mategka.dava.analyzer.spoon.action.EditActions;
import com.mategka.dava.analyzer.struct.*;
import com.mategka.dava.analyzer.struct.symbol.*;
import com.mategka.dava.analyzer.wip.ReflectionContext;

import gumtree.spoon.builder.CtWrapper;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.support.compiler.VirtualFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class App {

  public static void main(String[] args) {
    System.out.println("Hello World!");
    // ?REPO
    // ?REPO
    try (RepositoryWrapper repository = RepositoryWrapper.open("?REPO")) {
      Ref mainBranch = repository.resolveRef("main").orElseThrow();
      var reflectionContext = new ReflectionContext();
      var history = History.emptyOfBranch(repository, mainBranch);
      var timeBefore = System.currentTimeMillis();
      var symbolIdCounter = new AtomicLong();
      int offset = 0;
      var comparator = new AstComparator();
      //noinspection MismatchedQueryAndUpdateOfCollection
      Map<Strand, StrandWorkspace> workspaces = new DefaultMap<>(HashMap::new, StrandWorkspace::new);
      // TODO: Traverse commits in normal topological order for ~5% performance boost
      try (RevWalk walk = repository.commitsUpTo(mainBranch, CommitOrder.REVERSE_TOPOLOGICAL)) {
        for (RevCommit revCommit : walk) {
          var commit = new Commit(revCommit);
          var commitSha = commit.sha();
          var strand = history.getStrandMapping().get(commitSha);
          var workspace = workspaces.get(strand);
          System.out.print(commitSha.substring(0, 6) + " ");
          if (++offset >= 12) {
            offset = 0;
            System.out.println();
          }
          var parent = OptionalsX.getFirst(commit.parents());
          if (parent.isEmpty()) {
            // TODO: Fix initial commit variant algorithm
            var diffs = repository.initialCommitFilesOf(commit);
            var additions = RelevantDiffs.extract(diffs).get(FileChangeType.ADDED);
            if (additions.isEmpty()) {
              // No relevant changes
              continue;
            }
            var currentContents = workspace.getSpoonFiles();
            for (var diff : additions) {
              var content = repository.readFile(diff, Side.NEW).getSuccess().orElseThrow();
              var file = new VirtualFile(content, diff.getNewPath());
              currentContents.put(file.getPath(), file);
            }
            for (var file : currentContents.values()) {
              //workspace.getSpoonUnits().put(file, Spoon.parse(file));
            }
            // TODO: Process symbol additions
            continue;
          }
          var actualParent = parent.get();
          var parentStrand = history.getStrandMapping().get(actualParent.sha());
          var parentWorkspace = workspaces.get(parentStrand);
          var parentFiles = parentWorkspace.getSpoonFiles();
          try (DiffFormatter formatter = repository.newFormatter()) {
            var diffs = formatter.scan(actualParent.tree(), commit.tree());
            var relevantDiffs = RelevantDiffs.extract(diffs);
            Map<String, VirtualFile> overrideFiles = getOverrides(relevantDiffs, repository);
            if (overrideFiles.isEmpty()) {
              // No relevant changes
              continue;
            }
            Map<String, VirtualFile> effectiveFiles = new ChainMap<>(overrideFiles, parentFiles);
            Map<VirtualFile, CtCompilationUnit> effectiveUnits = effectiveFiles.values().stream()
              .filter(Objects::nonNull)
              .collect(CollectorsX.mapToValue(Spoon::parse));
            // START xdiffs
            var xdiffs = relevantDiffs.put(FileChangeType.RENAMED, new ArrayList<>());
            assert xdiffs != null;
            var xxdiffs = relevantDiffs.put(FileChangeType.MOVED, new ArrayList<>());
            assert xxdiffs != null;
            xdiffs.addAll(xxdiffs);
            relevantDiffs.get(FileChangeType.DELETED).addAll(xdiffs);
            relevantDiffs.get(FileChangeType.ADDED).addAll(xdiffs);
            var xxxdiffs = relevantDiffs.put(FileChangeType.COPIED, new ArrayList<>());
            assert xxxdiffs != null;
            relevantDiffs.get(FileChangeType.ADDED).addAll(xxxdiffs);
            // END xdiffs
            // TODO: Do not trust rename, move and copy hints from Git
            var derivativeDiffPairs = Stream.of(FileChangeType.RENAMED, FileChangeType.MOVED, FileChangeType.COPIED)
              .flatMap(t -> relevantDiffs.get(t).stream().map(d -> Pair.of(t, d)))
              .toList();
            var creationContext = new SymbolCreationContext(symbolIdCounter, strand.getId(), commitSha);
            var symbolizer = new Symbolizer(creationContext);
            List<Symbol> additions = new ArrayList<>();
            List<Symbol> deletions = new ArrayList<>();
            var updates = new IndexMap<>(HashMap::new, SymbolUpdate::getId);
            for (var diffPair : derivativeDiffPairs) {
              assert false; // TODO: Remove 10 xdiffs lines above and implement this
              var type = diffPair.left();
              var diff = diffPair.right();
              // Treat declared type as renamed symbol
              var oldUnit = parentWorkspace.getUnit(diff.getOldPath());
              var newUnit = effectiveUnits.get(overrideFiles.get(diff.getNewPath()));
              var astDiff = comparator.compare(oldUnit.getMainType(), newUnit.getMainType());
              var editScript = astDiff.getRootOperations();
              var mappings = astDiff.getMappingsComp();
              int dummy = 1;
            }
            for (var diff : relevantDiffs.get(FileChangeType.MODIFIED)) {
              var oldUnit = parentWorkspace.getUnit(diff.getOldPath());
              var newFile = overrideFiles.get(diff.getNewPath());
              var newUnit = effectiveUnits.get(newFile);
              var astDiff = comparator.compare(oldUnit.getMainType(), newUnit.getMainType());
              var editScript = astDiff.getRootOperations();
              var mappings = PairStream.mapping(
                  astDiff.getMappingsComp().asSet(),
                  m -> Pair.of(m.first, m.second)
                )
                .filterBoth(t -> !t.isRoot() && !t.getType().isEmpty())
                .mapBoth(Spoon::getMetaElement)
                .mapBoth(Optional::ofNullable)
                .mapBoth(o -> o.map(e -> e instanceof CtWrapper<?> ? null : e))
                .map(OptionalsX::pair)
                .mapMulti(OptionalsX.yieldIfPresent())
                .collect(CollectorsX.toBiMap());
              var actions = EditActions.fromDiff(astDiff, mappings);
              workspace.replaceFileEntry(diff.getOldPath(), newFile, newUnit);
              int dummy = 1;
            }
            for (var diff : relevantDiffs.get(FileChangeType.ADDED)) {
              var newFile = overrideFiles.get(diff.getNewPath());
              var newUnit = effectiveUnits.get(newFile);
              var packageDeclaration = newUnit.getPackageDeclaration().getReference().getDeclaration();
              var pakkage = workspaces.get(strand).getPackage(packageDeclaration, creationContext);
              var typeDeclaration = newUnit.getMainType();
              StreamsX.stepper(symbolizer.symbolizeType(typeDeclaration, pakkage))
                .takeOne(s -> {
                  workspace.putClassSymbol(new FileEntry(diff.getNewPath(), newFile, newUnit, s));
                  additions.add(s);
                })
                .forEachRemaining(s -> {
                  workspace.putSymbol(s);
                  additions.add(s);
                });
            }
            for (var diff : relevantDiffs.get(FileChangeType.DELETED)) {
              StreamsX.stepper(parentWorkspace.getSymbolsFromFilePath(diff.getOldPath()))
                .takeOne(s -> {
                  workspace.removeClassSymbolHierarchy(s);
                  deletions.add(s);
                })
                .forEachRemaining(deletions::add);
            }
            var commitDiff = CommitDiff.builder()
              .parentCommitShas(List.of(actualParent.sha()))
              .commitSha(commitSha)
              .commitDate(commit.dateTime())
              .successions(Collections.emptyMap())
              .refactorings(Collections.emptyList())
              .additions(additions)
              .deletions(deletions)
              .updates(updates)
              .build();
            strand.getCommitDiffs().add(commitDiff);
          }
        }
      }
      System.out.println("Done!");
      var time = System.currentTimeMillis() - timeBefore;
      System.out.println("Time (ms): " + time);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Map<String, VirtualFile> getOverrides(
    Map<FileChangeType, List<DiffEntry>> diffs,
    RepositoryWrapper repository
  ) {
    return diffs.entrySet().stream()
      .flatMap(e -> e.getValue().stream().map(d -> Pair.of(e.getKey(), d)))
      .flatMap(p -> {
        List<Pair<String, VirtualFile>> entries = new ArrayList<>();
        var type = p.left();
        var diff = p.right();
        if (type.isRemovingOldResource()) {
          entries.add(Pair.of(diff.getOldPath(), null));
        }
        if (type.isAddingNewResource()) {
          var newContent = repository.readFile(diff, Side.NEW).getSuccess().orElseThrow();
          var newFile = new VirtualFile(newContent, diff.getNewPath());
          entries.add(Pair.of(diff.getNewPath(), newFile));
        }
        return entries.stream();
      })
      .collect(CollectorsX.pairsToMap());
  }

}
