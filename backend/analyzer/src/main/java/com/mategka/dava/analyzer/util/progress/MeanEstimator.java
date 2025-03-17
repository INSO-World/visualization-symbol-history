package com.mategka.dava.analyzer.util.progress;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PROTECTED)
public abstract class MeanEstimator implements Estimator {

  long sum = 0;
  long count = 0;

  @Override
  public synchronized int estimate() {
    if (count == 0) {
      return 0;
    }
    return Math.clamp(sum / count, 0, Integer.MAX_VALUE);
  }

  @Override
  public synchronized void reset() {
    sum = 0;
    count = 0;
  }

}
