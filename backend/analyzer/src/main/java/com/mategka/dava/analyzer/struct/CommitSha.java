package com.mategka.dava.analyzer.struct;

import org.jetbrains.annotations.NotNull;

public record CommitSha(@NotNull String fullSha) {

  public String shortSha() {
    return fullSha.substring(0, 6);
  }

}
