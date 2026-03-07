package com.mategka.dava.analyzer.collections;

public record Mapping<S, T, M>(S source, T target, M metadata) {

  public boolean isAddition() {
    return source == null;
  }

  public boolean isDeletion() {
    return target == null;
  }

  public boolean isStatic() {
    return metadata == null;
  }

}
