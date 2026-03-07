package com.mategka.dava.analyzer.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Duration;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Benchmark {

  final long startTime;
  Long endTime;
  Duration duration;

  public static Benchmark start() {
    return new Benchmark(System.nanoTime());
  }

  public Duration end() {
    endTime = System.nanoTime();
    return duration = Duration.ofNanos(endTime - startTime);
  }

}
