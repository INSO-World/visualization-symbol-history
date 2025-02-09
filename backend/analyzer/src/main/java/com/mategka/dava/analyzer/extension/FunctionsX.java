package com.mategka.dava.analyzer.extension;

import lombok.experimental.UtilityClass;

import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

@SuppressWarnings({ "rawtypes", "unchecked" })
@UtilityClass
public class FunctionsX {

  private static final BinaryOperator TAKE_FIRST = (a, b) -> a;
  private static final BinaryOperator TAKE_SECOND = (a, b) -> b;
  private static final BiFunction TAKE_FIRST2 = (a, b) -> a;
  private static final BiFunction TAKE_SECOND2 = (a, b) -> b;

  public <T> BinaryOperator<T> takeFirst() {
    return (BinaryOperator<T>) TAKE_FIRST;
  }

  public <T> BinaryOperator<T> takeSecond() {
    return (BinaryOperator<T>) TAKE_SECOND;
  }

  public <A, B> BiFunction<A, B, A> takeFirst2() {
    return (BiFunction<A, B, A>) TAKE_FIRST2;
  }

  public <A, B> BiFunction<A, B, B> takeSecond2() {
    return (BiFunction<A, B, B>) TAKE_SECOND2;
  }

}
