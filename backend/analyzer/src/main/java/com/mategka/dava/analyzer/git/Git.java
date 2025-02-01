package com.mategka.dava.analyzer.git;

import lombok.experimental.UtilityClass;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@UtilityClass
public class Git {

  public @NotNull ZonedDateTime getCommitTime(@NotNull RevCommit commit) {
    var instant = Instant.ofEpochSecond(commit.getCommitTime());
    var zoneOffset = ZoneOffset.ofTotalSeconds(commit.getAuthorIdent().getTimeZoneOffset() * 60);
    return ZonedDateTime.ofInstant(instant, zoneOffset);
  }

}
