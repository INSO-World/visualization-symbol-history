package com.mategka.dava.analyzer.git;

import lombok.experimental.UtilityClass;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

@UtilityClass
public class Repositories {

  public Repository open(@NotNull String repositoryPath) throws IOException {
    return new RepositoryBuilder()
      .setMustExist(true)
      .setGitDir(getGitFolder(repositoryPath))
      .build();
  }

  private File getGitFolder(@NotNull String repositoryPath) throws FileNotFoundException {
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

}
