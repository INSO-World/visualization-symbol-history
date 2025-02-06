package com.mategka.dava.analyzer.git;

import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

public record Commit(@NotNull RevCommit revCommit) {

  public String sha() {
    return revCommit.getId().getName();
  }

  public List<Commit> parents() {
    return Arrays.stream(revCommit.getParents()).map(Commit::new).toList();
  }

  public ZonedDateTime dateTime() {
    var instant = Instant.ofEpochSecond(revCommit.getCommitTime());
    var zoneOffset = ZoneOffset.ofTotalSeconds(revCommit.getAuthorIdent().getTimeZoneOffset() * 60);
    return ZonedDateTime.ofInstant(instant, zoneOffset);
  }

  public RevTree tree() {
    return revCommit.getTree();
  }

}
