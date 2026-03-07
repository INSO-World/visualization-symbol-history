package com.mategka.dava.analyzer.git;

import lombok.Value;
import org.eclipse.jgit.revwalk.RevWalk;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Value
public class CommitWalk implements Iterable<Commit>, AutoCloseable {

  RevWalk revWalk;

  @Override
  public @NotNull Iterator<Commit> iterator() {
    return stream().iterator();
  }

  @Override
  public Spliterator<Commit> spliterator() {
    return stream().spliterator();
  }

  @Override
  public void close() {
    revWalk.close();
  }

  private Stream<Commit> stream() {
    return StreamSupport.stream(revWalk.spliterator(), false).map(Commit::new);
  }

}
