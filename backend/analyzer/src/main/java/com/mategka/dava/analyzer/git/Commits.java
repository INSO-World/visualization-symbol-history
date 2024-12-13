package com.mategka.dava.analyzer.git;

import lombok.experimental.UtilityClass;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.IOException;

@UtilityClass
public class Commits {

  public RevWalk topological(Repository repository, Ref head) throws IOException {
    return topological(repository, head, false);
  }

  public RevWalk topologicalReverse(Repository repository, Ref head) throws IOException {
    return topological(repository, head, true);
  }

  public RevWalk topological(Repository repository, Ref head, boolean parentsFirst) throws IOException {
    RevWalk revWalk = new RevWalk(repository);
    RevCommit startCommit = revWalk.parseCommit(head.getObjectId());
    revWalk.markStart(startCommit);
    revWalk.sort(RevSort.TOPO);
    revWalk.sort(RevSort.REVERSE, parentsFirst);
    return revWalk;
  }

}
