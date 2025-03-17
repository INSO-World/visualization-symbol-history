package com.mategka.dava.analyzer.util.progress;

import com.mategka.dava.analyzer.collections.SlidingWindow;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LocalMeanEstimator extends MeanEstimator {

  SlidingWindow<Integer> window;

  public LocalMeanEstimator(int windowSize) {
    window = new SlidingWindow<>(windowSize);
  }

  @Override
  public synchronized void accept(int value) {
    sum += value - window.accept(value);
    count++;
  }

  @Override
  public int getRetentionSize() {
    return window.capacity();
  }

  @Override
  public synchronized void reset() {
    super.reset();
    window.clear();
  }

}
