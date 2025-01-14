package com.mategka.dava.analyzer.git;

import lombok.experimental.UtilityClass;
import org.eclipse.jgit.diff.DiffEntry;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class Side {

  public final DiffEntry.Side OLD = DiffEntry.Side.OLD;
  public final DiffEntry.Side NEW = DiffEntry.Side.NEW;

  private final List<DiffEntry.Side> VALUES = Arrays.asList(DiffEntry.Side.values());

  public List<DiffEntry.Side> values() {
    return VALUES;
  }

}
