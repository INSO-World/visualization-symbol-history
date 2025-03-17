package com.mategka.dava.analyzer.git;

import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.option.Options;

import com.leakyabstractions.result.api.Result;
import com.leakyabstractions.result.core.Results;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.NullOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RepositoryWrapper implements AutoCloseable {

  Repository repository;

  public static RepositoryWrapper open(@NotNull String repositoryPath) throws IOException {
    var repository = new RepositoryBuilder()
      .setMustExist(true)
      .setGitDir(getGitFolder(repositoryPath))
      .build();
    return new RepositoryWrapper(repository);
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

  public CommitWalk commitsUpTo(@NotNull Ref head, CommitOrder order) throws IOException {
    var revWalk = new RevWalk(repository);
    var startCommit = revWalk.parseCommit(head.getObjectId());
    revWalk.markStart(startCommit);
    order.applyTo(revWalk);
    return new CommitWalk(revWalk);
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

  public DiffFormatter newFormatter() {
    DiffFormatter formatter = new DiffFormatter(NullOutputStream.INSTANCE);
    formatter.setRepository(repository);
    formatter.setContext(0);
    formatter.setDetectRenames(true);
    formatter.setDiffAlgorithm(DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.MYERS));
    var renameDetector = formatter.getRenameDetector();
    renameDetector.setRenameScore(50);
    return formatter;
  }

  public TreeWalk newTreeWalk(@NotNull Commit commit) throws IOException {
    var walk = new TreeWalk(repository);
    walk.addTree(commit.tree());
    walk.setRecursive(true);
    return walk;
  }

  public Result<String, IOException> readFile(DiffEntry diff, DiffEntry.Side side) {
    return readFile(diff.getId(side).toObjectId());
  }

  public Result<String, IOException> readFile(ObjectId objectId) {
    try (
      ObjectReader reader = repository.newObjectReader();
      ByteArrayOutputStream output = new ByteArrayOutputStream()
    ) {
      reader.open(objectId).copyTo(output);
      return Results.success(output.toString(StandardCharsets.UTF_8));
    } catch (IOException e) {
      return Results.failure(e);
    }
  }

  public Option<Ref> resolveRef(@NotNull String name) {
    return Options.fromCallable(() -> repository.findRef(name));
  }

  @Override
  public void close() {
    repository.close();
  }

}
