package com.mategka.dava.analyzer.extension;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

@UtilityClass
public class Paths2 {

  public boolean areSiblingPaths(@NotNull String path1, @NotNull String path2) {
    return Path.of(path1).getParent().equals(Path.of(path2).getParent());
  }

}
