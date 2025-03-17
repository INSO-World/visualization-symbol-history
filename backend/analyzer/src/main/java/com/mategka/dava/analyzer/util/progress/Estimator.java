package com.mategka.dava.analyzer.util.progress;

public interface Estimator {

  void accept(int value);

  int estimate();

  int getRetentionSize();

  void reset();

}
