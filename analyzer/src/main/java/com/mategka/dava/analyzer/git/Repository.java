package com.mategka.dava.analyzer.git;

import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.option.Options;

import com.leakyabstractions.result.api.Result;
import com.leakyabstractions.result.core.Results;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Repository implements AutoCloseable {

  org.eclipse.jgit.lib.Repository jgitRepository;

  public static Repository open(@NotNull String repositoryPath) throws IOException {
    var repository = new RepositoryBuilder()
      .setMustExist(true)
      .setGitDir(getGitFolder(repositoryPath))
      .build();
    return new Repository(repository);
  }

  private static File getGitFolder(@NotNull String repositoryPath) throws FileNotFoundException {
    File folder = new File(repositoryPath);
    if (!folder.exists() || !folder.isDirectory()) {
      throw new FileNotFoundException(repositoryPath);
    }
    Path currentPath = folder.toPath();
    while (currentPath != null) {
      File gitFolder = currentPath.resolve(".git").toFile();
      if (gitFolder.exists() && gitFolder.isDirectory()) {
        return gitFolder;
      }
      currentPath = currentPath.getParent();
    }
    throw new FileNotFoundException(folder.getPath());
  }

  public CommitWalk commitsUpTo(@NotNull ObjectId head, CommitOrder order) throws IOException {
    var revWalk = new RevWalk(jgitRepository);
    var startCommit = revWalk.parseCommit(head);
    revWalk.markStart(startCommit);
    order.applyTo(revWalk);
    return new CommitWalk(revWalk);
  }

  public String getName() {
    return jgitRepository.getWorkTree().getName();
  }

  public List<DiffEntry> initialCommitFilesOf(@NotNull Commit commit) throws IOException {
    var result = new ArrayList<DiffEntry>();
    try (var walk = newTreeWalk(commit)) {
      while (walk.next()) {
        result.add(DiffEntries.newAddition(walk.getPathString(), walk.getObjectId(0)));
      }
    }
    return result;
  }

  public TreeDiffer newTreeDiffer() {
    return TreeDiffer.create(this);
  }

  public TreeWalk newTreeWalk(@NotNull Commit commit) throws IOException {
    var walk = new TreeWalk(jgitRepository);
    walk.addTree(commit.tree());
    walk.setRecursive(true);
    return walk;
  }

  public Result<String, IOException> readFile(DiffEntry diff, DiffEntry.Side side) {
    return readFile(diff.getId(side).toObjectId());
  }

  public Result<String, IOException> readFile(ObjectId objectId) {
    try (
      ObjectReader reader = jgitRepository.newObjectReader();
      ByteArrayOutputStream output = new ByteArrayOutputStream()
    ) {
      reader.open(objectId).copyTo(output);
      return Results.success(output.toString(StandardCharsets.UTF_8));
    } catch (IOException e) {
      return Results.failure(e);
    }
  }

  public Set<String> readRelevantPaths(@NotNull Commit commit) {
    Set<String> result = new HashSet<>();
    try (var walk = newTreeWalk(commit)) {
      // FUTURE: Add support for subtrees/submodules, which will currently make next() throw
      while (walk.next()) {
        var path = walk.getPathString();
        if (RelevantDiffs.isFileRelevant(path)) {
          result.add(path);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read paths from " + commit, e);
    }
    return result;
  }

  public Option<ObjectId> resolveObjectId(@NotNull String name) {
    return Options.fromCallable(() -> jgitRepository.resolve(name));
  }

  @Override
  public void close() {
    jgitRepository.close();
  }

}
