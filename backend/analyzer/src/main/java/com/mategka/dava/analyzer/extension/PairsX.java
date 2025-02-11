package com.mategka.dava.analyzer.extension;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.BiFunction;
import java.util.function.Function;

@UtilityClass
public class PairsX {

  public <L, R> Function<L, Pair<L, R>> mapToRight(Function<? super L, ? extends R> mapper) {
    return l -> Pair.of(l, mapper.apply(l));
  }

  public <L, R> Function<R, Pair<L, R>> mapToLeft(Function<? super R, ? extends L> mapper) {
    return r -> Pair.of(mapper.apply(r), r);
  }

  public <L1, L2, R> Function<Pair<L1, R>, Pair<L2, R>> mapLeft(Function<? super L1, ? extends L2> mapper) {
    return mapBoth(mapper, Function.identity());
  }

  public <L, R1, R2> Function<Pair<L, R1>, Pair<L, R2>> mapRight(Function<? super R1, ? extends R2> mapper) {
    return mapBoth(Function.identity(), mapper);
  }

  public <L1, L2, R1, R2> Function<Pair<L1, R1>, Pair<L2, R2>> mapBoth(
    Function<? super L1, ? extends L2> leftMapper,
    Function<? super R1, ? extends R2> rightMapper
  ) {
    return p -> Pair.of(leftMapper.apply(p.getLeft()), rightMapper.apply(p.getRight()));
  }

  public <T1, T2> Function<Pair<T1, T1>, Pair<T2, T2>> mapBoth(Function<? super T1, ? extends T2> mapper) {
    return mapBoth(mapper, mapper);
  }

  public <L, R, T> Function<Pair<L, R>, T> reduce(BiFunction<? super L, ? super R, ? extends T> mapper) {
    return p -> mapper.apply(p.getLeft(), p.getRight());
  }

}
