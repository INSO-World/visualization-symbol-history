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

  public ZonedDateTime dateTime() {
    assertParsed();
    var instant = Instant.ofEpochSecond(revCommit.getCommitTime());
    var zoneOffset = ZoneOffset.ofTotalSeconds(revCommit.getAuthorIdent().getTimeZoneOffset() * 60);
    return ZonedDateTime.ofInstant(instant, zoneOffset);
  }

  public void disposeBody() {
    revCommit.disposeBody();
  }

  public List<Commit> parents() {
    return Arrays.stream(revCommit.getParents()).map(Commit::new).toList();
  }

  public String sha() {
    return revCommit.getId().getName();
  }

  public String shortSha() {
    return sha().substring(0, 6);
  }

  public String summary() {
    assertParsed();
    return revCommit.getShortMessage();
  }

  public RevTree tree() {
    return revCommit.getTree();
  }

  private void assertParsed() {
    if (revCommit.getRawBuffer() == null) {
      throw new IllegalStateException("Commit has not been parsed");
    }
  }

}
