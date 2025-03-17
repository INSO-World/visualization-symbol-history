package com.mategka.dava.analyzer.util.progress;

public final class MainTask extends CompoundTask {

  public MainTask(long subtaskCount) {
    super(1, subtaskCount);
  }

}
