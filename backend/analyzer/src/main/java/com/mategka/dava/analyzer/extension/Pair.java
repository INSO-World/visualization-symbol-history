package com.mategka.dava.analyzer.extension;

import com.mategka.dava.analyzer.collections.MapEntry;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(staticName = "of")
@ToString
@EqualsAndHashCode
public class Pair<L, R> implements Comparable<Pair<L, R>> {

  L left;
  R right;

  public static <T, U> Pair<U, U> map(Pair<? extends T, ? extends T> pair, Function<? super T, ? extends U> mapper) {
    return new Pair<>(mapper.apply(pair.left), mapper.apply(pair.right));
  }

  public static <L, R> Function<L, Pair<L, R>> fromLeft(Function<? super L, ? extends R> mapper) {
    return l -> Pair.of(l, mapper.apply(l));
  }

  public static <L, R> Function<R, Pair<L, R>> fromRight(Function<? super R, ? extends L> mapper) {
    return r -> Pair.of(mapper.apply(r), r);
  }

  public static <L, R, L2, R2> Function<Pair<? extends L, ? extends R>, Pair<L2, R2>> mapping(
    Function<? super L, ? extends L2> mapper1, Function<? super R, ? extends R2> mapper2) {
    return pair -> pair.map(mapper1, mapper2);
  }

  public static <L, R, L2> Function<Pair<? extends L, R>, Pair<L2, R>> mappingLeft(
    Function<? super L, ? extends L2> mapper1) {
    return pair -> pair.mapLeft(mapper1);
  }

  public static <L, R, R2> Function<Pair<L, ? extends R>, Pair<L, R2>> mappingRight(
    Function<? super R, ? extends R2> mapper2) {
    return pair -> pair.mapRight(mapper2);
  }

  public static <T, U> Function<Pair<? extends T, ? extends T>, Pair<U, U>> mapping(
    Function<? super T, ? extends U> mapper) {
    return pair -> map(pair, mapper);
  }

  public L left() {
    return left;
  }

  public R right() {
    return right;
  }

  public Map.Entry<L, R> asEntry() {
    return MapEntry.of(left, right);
  }

  public Pair<R, L> inverse() {
    return new Pair<>(right, left);
  }

  public <L2> Pair<L2, R> mapLeft(Function<? super L, ? extends L2> mapper1) {
    return map(mapper1, Function.identity());
  }

  public <R2> Pair<L, R2> mapRight(Function<? super R, ? extends R2> mapper2) {
    return map(Function.identity(), mapper2);
  }

  public <L2, R2> Pair<L2, R2> map(Function<? super L, ? extends L2> mapper1,
                                   Function<? super R, ? extends R2> mapper2) {
    return new Pair<>(mapper1.apply(left), mapper2.apply(right));
  }

  public <T> T fold(BiFunction<? super L, ? super R, ? extends T> mapper) {
    return mapper.apply(left, right);
  }

  @Override
  public int compareTo(@NotNull Pair<L, R> o) {
    var comparator = ObjectsX.nullsFirstComparator();
    var diffLeft = comparator.compare(left, o.left);
    return (diffLeft != 0) ? diffLeft : comparator.compare(right, o.right);
  }

}
