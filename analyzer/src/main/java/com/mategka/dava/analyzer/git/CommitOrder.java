package com.mategka.dava.analyzer.git;

import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;

public enum CommitOrder {

  TOPOLOGICAL(RevSort.TOPO),
  REVERSE_TOPOLOGICAL(RevSort.TOPO, RevSort.REVERSE),
  ;

  private final RevSort[] sortFlags;

  CommitOrder(RevSort... sortFlags) {
    this.sortFlags = sortFlags;
  }

  public void applyTo(RevWalk revWalk) {
    revWalk.sort(RevSort.NONE);
    for (RevSort sortFlag : sortFlags) {
      revWalk.sort(sortFlag, true);
    }
  }

}
