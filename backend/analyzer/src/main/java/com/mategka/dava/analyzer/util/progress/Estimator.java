package com.mategka.dava.analyzer.util.progress;

public interface Estimator {

  int getRetentionSize();

  void accept(int value);

  void reset();

  int estimate();

}
