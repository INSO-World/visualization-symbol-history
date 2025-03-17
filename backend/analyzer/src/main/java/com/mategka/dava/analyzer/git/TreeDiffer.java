package com.mategka.dava.analyzer.git;

import com.mategka.dava.analyzer.collections.MultisetArray;
import com.mategka.dava.analyzer.diff.file.FileChange;
import com.mategka.dava.analyzer.extension.stream.AnStream;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.util.io.NullOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(staticName = "create")
@EqualsAndHashCode
@ToString
public class TreeDiffer {

  Repository repository;

  public DiffFormatter newFormatter() {
    DiffFormatter formatter = new DiffFormatter(NullOutputStream.INSTANCE);
    formatter.setRepository(repository.getSpoonRepository());
    formatter.setContext(0);
    formatter.setDetectRenames(true);
    formatter.setDiffAlgorithm(DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.MYERS));
    var renameDetector = formatter.getRenameDetector();
    renameDetector.setRenameScore(50);
    return formatter;
  }

  public List<DiffEntry> diff(@NotNull Commit before, @NotNull Commit after) {
    try (var diffFormatter = newFormatter()) {
      return diffFormatter.scan(before.tree(), after.tree());
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not determine differences for " + after, e);
    }
  }

  public MultisetArray<FileChange> diffRelevant(@NotNull Commit after) {
    var parents = after.parents();
    var result = MultisetArray.<FileChange>create(parents.size());
    for (int parentIndex = 0; parentIndex < parents.size(); parentIndex++) {
      var parent = parents.get(parentIndex);
      var diffs = diff(parent, after);
      var relevantDiffs = AnStream.from(diffs)
        .filter(RelevantDiffs::isDiffRelevant)
        .map(d -> new FileChange(RelevantDiffs.getChangeType(d), d))
        .toList();
      result.putAll(parentIndex, relevantDiffs);
    }
    return result;
  }

}
