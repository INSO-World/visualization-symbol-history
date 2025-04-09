package com.mategka.dava.analyzer.diff.file;

import com.mategka.dava.analyzer.collections.Array;
import com.mategka.dava.analyzer.collections.ManyToManyMap;
import com.mategka.dava.analyzer.collections.Mapping;
import com.mategka.dava.analyzer.extension.ListsX;
import com.mategka.dava.analyzer.extension.stream.AnStream;

import lombok.Value;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class FileMapping {

  /**
   * The stored mappings from source (parent) to target (child) paths.
   * <p>
   * Note that additions have a {@code null} path in their source {@linkplain ParentFile},
   * deletions map <i>to</i> the {@code null} path, and unchanged files have a {@code null} {@linkplain FileChange}.
   */
  ManyToManyMap<ParentFile, String, FileChange> mappings;

  public static boolean isFileAddition(Mapping<ParentFile, ?, ?> fileMapping) {
    return fileMapping.source().filePath() == null;
  }

  /**
   * Also add mappings for files that have remained unchanged.
   * Unchanged files are all those which are not already mapped and exist under an identical path in the child commit.
   * After this, all source and target paths should be mapped.
   * Note that calling this method is only useful for breakpoint commits (first-in-strand) as unchanged files are of no
   * interest for intra-strand comparisons (the local model just won't be updated).
   */
  public void addUnchangedMappings(Array<Collection<String>> parentPathsPerParent, Set<String> childPaths) {
    for (int parentIndex = 0; parentIndex < parentPathsPerParent.length; parentIndex++) {
      addUnchangedMappingsForParent(parentIndex, parentPathsPerParent.get(parentIndex), childPaths);
    }
  }

  public List<ParentFile> getDeletedFiles() {
    return mappings.mappings().stream()
      .filter(Mapping::isDeletion)
      .map(Mapping::source)
      .collect(Collectors.toList());
  }

  public List<ParentFile> getUnchangedFiles() {
    return mappings.mappings().stream()
      .filter(Mapping::isStatic)
      .map(Mapping::source)
      .collect(Collectors.toList());
  }

  private void addUnchangedMappingsForParent(int parentIndex, Collection<String> parentPaths, Set<String> childPaths) {
    var parentFiles = ListsX.map(parentPaths, p -> new ParentFile(parentIndex, p));
    var unchangedFiles = AnStream.from(mappings.getUnmappedSources(parentFiles))
      .filterBy(ParentFile::filePath, childPaths::contains)
      .toList();
    for (var file : unchangedFiles) {
      mappings.put(file, file.filePath(), null);
    }
  }

  /*
  Types when looking at mappings:
  - If a new path is mapped for some parents -> mapping
    - Note: If a new path is unmapped as an old path at some parent and that parents maps no other old path to this new path, then this is a deletion contribution
    - If a new path is unmapped by a parent -> addition contribution
  - Paths with only addition contributions are strict additions and the DiffEntry for any parent may be used
  - Paths with only deletion contributions are strict deletions (but the DiffEntry objects must all stay intact)
  - Paths with only modification contributions are strict modifications (DiffEntry objects intact)
  - Paths with only a single contribution are strict successions

  New Path -> Index -> (ChangeType, DiffEntry)
   */

}
