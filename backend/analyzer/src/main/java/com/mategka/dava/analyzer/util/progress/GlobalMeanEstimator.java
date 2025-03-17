package com.mategka.dava.analyzer.util.progress;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class GlobalMeanEstimator extends MeanEstimator {

  @Override
  public int getRetentionSize() {
    return Integer.MAX_VALUE;
  }

  @Override
  public synchronized void accept(int value) {
    sum += value;
    count++;
  }

}
