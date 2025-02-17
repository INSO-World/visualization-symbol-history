package com.mategka.dava.analyzer.extension;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PairStream<T> extends AbstractStreamAdapter<Pair<T, T>, PairStream<T>> {

  private PairStream(Stream<Pair<T, T>> stream) {
    super(stream, PairStream::new);
  }

  public static <T> PairStream<T> empty() {
    return new PairStream<>(Stream.empty());
  }

  public static <T> PairStream<T> fromStream(Stream<? extends Pair<? extends T, ? extends T>> stream) {
    return new PairStream<>(Covariant.stream(stream).map(Covariant::pair));
  }

  public static <T, U> PairStream<T> mapping(Collection<U> collection,
                                             Function<? super U, ? extends Pair<? extends T, ? extends T>> mapper) {
    return mapping(collection.stream(), mapper);
  }

  public static <T, U> PairStream<T> mapping(Stream<U> stream,
                                             Function<? super U, ? extends Pair<? extends T, ? extends T>> mapper) {
    //noinspection unchecked
    return new PairStream<>(stream.map((Function<? super U, ? extends Pair<T, T>>) mapper));
  }

  public static <T> PairStream<T> of(T left, T right) {
    return of(Pair.of(left, right));
  }

  public static <T> PairStream<T> of(Pair<? extends T, ? extends T> pair) {
    return new PairStream<>(Stream.of(Covariant.pair(pair)));
  }

  @SafeVarargs
  public static <T> PairStream<T> of(Pair<? extends T, ? extends T>... pairs) {
    return new PairStream<>(Arrays.stream(pairs).map(Covariant::pair));
  }

  public static <T> PairStream<T> zip(Stream<? extends T> streamA, Stream<? extends T> streamB) {
    return new PairStream<>(PairStream.of(streamA, streamB)
                              .mapBoth(Covariant::<T>stream)
                              .flatMap(StreamsX::zip)
    );
  }

  public PairStream<T> filterLeft(Predicate<? super T> predicate) {
    return filter(p -> predicate.test(p.left()));
  }

  public PairStream<T> filterRight(Predicate<? super T> predicate) {
    return filter(p -> predicate.test(p.right()));
  }

  public PairStream<T> filterEither(Predicate<? super T> predicate) {
    return filterEither(predicate, predicate);
  }

  public PairStream<T> filterEither(Predicate<? super T> leftPredicate, Predicate<? super T> rightPredicate) {
    return filter(p -> leftPredicate.test(p.left()) || rightPredicate.test(p.right()));
  }

  public PairStream<T> filterBoth(Predicate<? super T> predicate) {
    return filterBoth(predicate, predicate);
  }

  public PairStream<T> filterBoth(Predicate<? super T> leftPredicate, Predicate<? super T> rightPredicate) {
    return filter(p -> leftPredicate.test(p.left()) && rightPredicate.test(p.right()));
  }

  @Override
  public <R> Stream<R> map(Function<? super Pair<T, T>, ? extends R> mapper) {
    return stream.map(mapper);
  }

  public <R> Stream<R> mapReduce(BiFunction<? super T, ? super T, ? extends R> mapper) {
    return map(p -> mapper.apply(p.left(), p.right()));
  }

  public PairStream<T> mapLeft(Function<? super T, ? extends T> mapper) {
    return mapBoth(mapper, Function.identity());
  }

  public PairStream<T> mapRight(Function<? super T, ? extends T> mapper) {
    return mapBoth(Function.identity(), mapper);
  }

  public <U> PairStream<U> mapBoth(Function<? super T, ? extends U> mapper) {
    return mapBoth(mapper, mapper);
  }

  public <U> PairStream<U> mapBoth(Function<? super T, ? extends U> leftMapper,
                                   Function<? super T, ? extends U> rightMapper) {
    return new PairStream<>(map(p -> Pair.of(
      leftMapper.apply(p.left()),
      rightMapper.apply(p.right())
    )));
  }

  public <U> PairStream<U> mapPair(Function<? super Pair<T, T>, ? extends Pair<? extends U, ? extends U>> mapper) {
    //noinspection unchecked
    return new PairStream<>(map((Function<? super Pair<T, T>, ? extends Pair<U, U>>) mapper));
  }

  public IntStream mapToInt(ToIntBiFunction<? super T, ? super T> mapper) {
    return mapToInt(p -> mapper.applyAsInt(p.left(), p.right()));
  }

  public LongStream mapToLong(ToLongBiFunction<? super T, ? super T> mapper) {
    return mapToLong(p -> mapper.applyAsLong(p.left(), p.right()));
  }

  public DoubleStream mapToDouble(ToDoubleBiFunction<? super T, ? super T> mapper) {
    return mapToDouble(p -> mapper.applyAsDouble(p.left(), p.right()));
  }

  @Override
  public <R> Stream<R> flatMap(Function<? super Pair<T, T>, ? extends Stream<? extends R>> mapper) {
    return stream.flatMap(mapper);
  }

  public <R> Stream<R> flatMap(BiFunction<? super T, ? super T, ? extends Stream<? extends R>> mapper) {
    return flatMap(p -> mapper.apply(p.left(), p.right()));
  }

  public <R> PairStream<R> flatMapToPairs(
    Function<? super Pair<T, T>, ? extends Stream<? extends Pair<? extends R, ? extends R>>> mapper) {
    //noinspection unchecked
    return new PairStream<>(flatMap((Function<? super Pair<T, T>, ? extends Stream<? extends Pair<R, R>>>) mapper));
  }

  public <R> PairStream<R> flatMapToPairs(
    BiFunction<? super T, ? super T, ? extends Stream<? extends Pair<? extends R, ? extends R>>> mapper) {
    return flatMapToPairs(p -> mapper.apply(p.left(), p.right()));
  }

  public IntStream flatMapToInt(BiFunction<? super T, ? super T, ? extends IntStream> mapper) {
    return flatMapToInt(p -> mapper.apply(p.left(), p.right()));
  }

  public LongStream flatMapToLong(BiFunction<? super T, ? super T, ? extends LongStream> mapper) {
    return flatMapToLong(p -> mapper.apply(p.left(), p.right()));
  }

  public DoubleStream flatMapToDouble(BiFunction<? super T, ? super T, ? extends DoubleStream> mapper) {
    return flatMapToDouble(p -> mapper.apply(p.left(), p.right()));
  }

  public PairStream<T> sorted(ToIntBiFunction<? super T, ? super T> comparator) {
    return sorted(Comparator.comparingInt(p -> comparator.applyAsInt(p.left(), p.right())));
  }

  public PairStream<T> peek(BiConsumer<? super T, ? super T> action) {
    return peek(p -> action.accept(p.left(), p.right()));
  }

  public void forEach(BiConsumer<? super T, ? super T> action) {
    forEach(p -> action.accept(p.left(), p.right()));
  }

  public void forEachOrdered(BiConsumer<? super T, ? super T> action) {
    forEachOrdered(p -> action.accept(p.left(), p.right()));
  }

  public @NotNull Optional<Pair<T, T>> min() {
    return min(Comparator.nullsFirst(Comparator.naturalOrder()));
  }

  public @NotNull Optional<Pair<T, T>> min(ToIntBiFunction<? super T, ? super T> comparator) {
    return min(Comparator.comparingInt(p -> comparator.applyAsInt(p.left(), p.right())));
  }

  public @NotNull Optional<Pair<T, T>> max() {
    return max(Comparator.nullsLast(Comparator.naturalOrder()));
  }

  public @NotNull Optional<Pair<T, T>> max(ToIntBiFunction<? super T, ? super T> comparator) {
    return max(Comparator.comparingInt(p -> comparator.applyAsInt(p.left(), p.right())));
  }

  public boolean anyMatchLeft(Predicate<? super T> predicate) {
    return map(Pair::left).anyMatch(predicate);
  }

  public boolean anyMatchRight(Predicate<? super T> predicate) {
    return map(Pair::right).anyMatch(predicate);
  }

  public boolean anyMatchEither(Predicate<? super T> predicate) {
    return anyMatchEither(predicate, predicate);
  }

  public boolean anyMatchEither(Predicate<? super T> leftPredicate, Predicate<? super T> rightPredicate) {
    return filterEither(leftPredicate, rightPredicate).findAny().isPresent();
  }

  public boolean anyMatchBoth(Predicate<? super T> predicate) {
    return anyMatchBoth(predicate, predicate);
  }

  public boolean anyMatchBoth(Predicate<? super T> leftPredicate, Predicate<? super T> rightPredicate) {
    return filterBoth(leftPredicate, rightPredicate).findAny().isPresent();
  }

  public boolean allMatchLeft(Predicate<? super T> predicate) {
    return map(Pair::left).allMatch(predicate);
  }

  public boolean allMatchRight(Predicate<? super T> predicate) {
    return map(Pair::right).allMatch(predicate);
  }

  public boolean allMatchEither(Predicate<? super T> predicate) {
    return allMatchEither(predicate, predicate);
  }

  public boolean allMatchEither(Predicate<? super T> leftPredicate, Predicate<? super T> rightPredicate) {
    return !anyMatchBoth(Predicate.not(leftPredicate), Predicate.not(rightPredicate));
  }

  public boolean allMatchBoth(Predicate<? super T> predicate) {
    return allMatchBoth(predicate, predicate);
  }

  public boolean allMatchBoth(Predicate<? super T> leftPredicate, Predicate<? super T> rightPredicate) {
    return !anyMatchEither(Predicate.not(leftPredicate), Predicate.not(rightPredicate));
  }

  public boolean noneMatchLeft(Predicate<? super T> predicate) {
    return map(Pair::left).noneMatch(predicate);
  }

  public boolean noneMatchRight(Predicate<? super T> predicate) {
    return map(Pair::right).noneMatch(predicate);
  }

  public boolean noneMatchEither(Predicate<? super T> predicate) {
    return noneMatchEither(predicate, predicate);
  }

  public boolean noneMatchEither(Predicate<? super T> leftPredicate, Predicate<? super T> rightPredicate) {
    return !anyMatchEither(leftPredicate, rightPredicate);
  }

  public boolean noneMatchBoth(Predicate<? super T> predicate) {
    return noneMatchBoth(predicate, predicate);
  }

  public boolean noneMatchBoth(Predicate<? super T> leftPredicate, Predicate<? super T> rightPredicate) {
    return !anyMatchBoth(leftPredicate, rightPredicate);
  }

  public @NotNull Pair<T, T> toPair() {
    return findFirst().orElseThrow();
  }

}
