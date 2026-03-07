package com.mategka.dava.analyzer.git;

import com.mategka.dava.analyzer.extension.ListsX;

import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@EqualsAndHashCode
public final class Commit {

  private static final String DESCRIPTION_SEPARATOR = "\n\n";

  @NonNull
  RevCommit revCommit;

  @Accessors(fluent = true)
  @Getter(lazy = true)
  Hash hash = new Hash(revCommit.getId().getName());

  public AuthorInfo author() {
    assertParsed();
    var author = revCommit.getAuthorIdent();
    return new AuthorInfo(author.getName(), author.getEmailAddress());
  }

  public ZonedDateTime dateTime() {
    assertParsed();
    var instant = Instant.ofEpochSecond(revCommit.getCommitTime());
    var zoneOffset = ZoneOffset.ofTotalSeconds(revCommit.getAuthorIdent().getTimeZoneOffset() * 60);
    return ZonedDateTime.ofInstant(instant, zoneOffset);
  }

  /**
   * Disposes all information other than what is necessary for {@link #hash()}, {@link #parents()} and {@link #tree()}.
   */
  public void disposeBody() {
    revCommit.disposeBody();
  }

  @Contract(" -> new")
  public @NotNull CommitInfo info() {
    assertParsed();
    var message = revCommit.getFullMessage();
    var summaryEnd = message.indexOf(DESCRIPTION_SEPARATOR);
    summaryEnd = (summaryEnd == -1) ? message.length() : summaryEnd;
    var summary = message.substring(0, summaryEnd).trim();
    var description = message.substring(Math.min(message.length(), summaryEnd + 2)).trim();
    return new CommitInfo(hash(), summary, description, dateTime(), author(), ListsX.map(parents(), Commit::hash));
  }

  public List<Commit> parents() {
    return Arrays.stream(revCommit.getParents()).map(Commit::new).toList();
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
