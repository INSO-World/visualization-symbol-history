package com.mategka.dava.analyzer.extension;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PairStream<T> implements Stream<Pair<T, T>> {

  Stream<Pair<T, T>> stream;

  public static <T> PairStream<T> empty() {
    return new PairStream<>(Stream.empty());
  }

  public static <T> PairStream<T> fromStream(Stream<? extends Pair<? extends T, ? extends T>> stream) {
    return new PairStream<>(Covariant.stream(stream).map(Covariant::pair));
  }

  public static <T, U> PairStream<T> mapping(Collection<U> collection, Function<? super U, ? extends Pair<? extends T, ? extends T>> mapper) {
    return mapping(collection.stream(), mapper);
  }

  public static <T, U> PairStream<T> mapping(Stream<U> stream, Function<? super U, ? extends Pair<? extends T, ? extends T>> mapper) {
    //noinspection unchecked
    return new PairStream<>(stream.map((Function<? super U, ? extends Pair<T,T>>) mapper));
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

  @Override
  public PairStream<T> filter(Predicate<? super Pair<T, T>> predicate) {
    return new PairStream<>(stream.filter(predicate));
  }

  public PairStream<T> filterLeft(Predicate<? super T> predicate) {
    return filter(p -> predicate.test(p.getLeft()));
  }

  public PairStream<T> filterRight(Predicate<? super T> predicate) {
    return filter(p -> predicate.test(p.getRight()));
  }

  public PairStream<T> filterEither(Predicate<? super T> predicate) {
    return filterEither(predicate, predicate);
  }

  public PairStream<T> filterEither(Predicate<? super T> leftPredicate, Predicate<? super T> rightPredicate) {
    return filter(p -> leftPredicate.test(p.getLeft()) || rightPredicate.test(p.getRight()));
  }

  public PairStream<T> filterBoth(Predicate<? super T> predicate) {
    return filterBoth(predicate, predicate);
  }

  public PairStream<T> filterBoth(Predicate<? super T> leftPredicate, Predicate<? super T> rightPredicate) {
    return filter(p -> leftPredicate.test(p.getLeft()) && rightPredicate.test(p.getRight()));
  }

  @Override
  public <R> Stream<R> map(Function<? super Pair<T, T>, ? extends R> mapper) {
    return stream.map(mapper);
  }

  public <R> Stream<R> mapReduce(BiFunction<? super T, ? super T, ? extends R> mapper) {
    return map(p -> mapper.apply(p.getLeft(), p.getRight()));
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
      leftMapper.apply(p.getLeft()),
      rightMapper.apply(p.getRight())
    )));
  }

  public <U> PairStream<U> mapPair(Function<? super Pair<T, T>, ? extends Pair<? extends U, ? extends U>> mapper) {
    //noinspection unchecked
    return new PairStream<>(map((Function<? super Pair<T, T>, ? extends Pair<U, U>>) mapper));
  }

  @Override
  public IntStream mapToInt(ToIntFunction<? super Pair<T, T>> mapper) {
    return stream.mapToInt(mapper);
  }

  public IntStream mapToInt(ToIntBiFunction<? super T, ? super T> mapper) {
    return mapToInt(p -> mapper.applyAsInt(p.getLeft(), p.getRight()));
  }

  @Override
  public LongStream mapToLong(ToLongFunction<? super Pair<T, T>> mapper) {
    return stream.mapToLong(mapper);
  }

  public LongStream mapToLong(ToLongBiFunction<? super T, ? super T> mapper) {
    return mapToLong(p -> mapper.applyAsLong(p.getLeft(), p.getRight()));
  }

  @Override
  public DoubleStream mapToDouble(ToDoubleFunction<? super Pair<T, T>> mapper) {
    return stream.mapToDouble(mapper);
  }

  public DoubleStream mapToDouble(ToDoubleBiFunction<? super T, ? super T> mapper) {
    return mapToDouble(p -> mapper.applyAsDouble(p.getLeft(), p.getRight()));
  }

  @Override
  public <R> Stream<R> flatMap(Function<? super Pair<T, T>, ? extends Stream<? extends R>> mapper) {
    return stream.flatMap(mapper);
  }

  public <R> Stream<R> flatMap(BiFunction<? super T, ? super T, ? extends Stream<? extends R>> mapper) {
    return flatMap(p -> mapper.apply(p.getLeft(), p.getRight()));
  }

  public <R> PairStream<R> flatMapToPairs(
    Function<? super Pair<T, T>, ? extends Stream<? extends Pair<? extends R, ? extends R>>> mapper) {
    //noinspection unchecked
    return new PairStream<>(flatMap((Function<? super Pair<T, T>, ? extends Stream<? extends Pair<R, R>>>) mapper));
  }

  public <R> PairStream<R> flatMapToPairs(
    BiFunction<? super T, ? super T, ? extends Stream<? extends Pair<? extends R, ? extends R>>> mapper) {
    return flatMapToPairs(p -> mapper.apply(p.getLeft(), p.getRight()));
  }

  @Override
  public IntStream flatMapToInt(Function<? super Pair<T, T>, ? extends IntStream> mapper) {
    return stream.flatMapToInt(mapper);
  }

  public IntStream flatMapToInt(BiFunction<? super T, ? super T, ? extends IntStream> mapper) {
    return flatMapToInt(p -> mapper.apply(p.getLeft(), p.getRight()));
  }

  @Override
  public LongStream flatMapToLong(Function<? super Pair<T, T>, ? extends LongStream> mapper) {
    return stream.flatMapToLong(mapper);
  }

  public LongStream flatMapToLong(BiFunction<? super T, ? super T, ? extends LongStream> mapper) {
    return flatMapToLong(p -> mapper.apply(p.getLeft(), p.getRight()));
  }

  @Override
  public DoubleStream flatMapToDouble(Function<? super Pair<T, T>, ? extends DoubleStream> mapper) {
    return stream.flatMapToDouble(mapper);
  }

  public DoubleStream flatMapToDouble(BiFunction<? super T, ? super T, ? extends DoubleStream> mapper) {
    return flatMapToDouble(p -> mapper.apply(p.getLeft(), p.getRight()));
  }

  @Override
  public PairStream<T> distinct() {
    return new PairStream<>(stream.distinct());
  }

  @Override
  public PairStream<T> sorted() {
    return new PairStream<>(stream.sorted());
  }

  @Override
  public PairStream<T> sorted(Comparator<? super Pair<T, T>> comparator) {
    return new PairStream<>(stream.sorted(comparator));
  }

  public PairStream<T> sorted(ToIntBiFunction<? super T, ? super T> comparator) {
    return sorted(Comparator.comparingInt(p -> comparator.applyAsInt(p.getLeft(), p.getRight())));
  }

  @Override
  public PairStream<T> peek(Consumer<? super Pair<T, T>> action) {
    return new PairStream<>(stream.peek(action));
  }

  public PairStream<T> peek(BiConsumer<? super T, ? super T> action) {
    return peek(p -> action.accept(p.getLeft(), p.getRight()));
  }

  @Override
  public PairStream<T> limit(long maxSize) {
    return new PairStream<>(stream.limit(maxSize));
  }

  @Override
  public PairStream<T> skip(long n) {
    return new PairStream<>(stream.skip(n));
  }

  @Override
  public void forEach(Consumer<? super Pair<T, T>> action) {
    stream.forEach(action);
  }

  public void forEach(BiConsumer<? super T, ? super T> action) {
    forEach(p -> action.accept(p.getLeft(), p.getRight()));
  }

  @Override
  public void forEachOrdered(Consumer<? super Pair<T, T>> action) {
    stream.forEachOrdered(action);
  }

  public void forEachOrdered(BiConsumer<? super T, ? super T> action) {
    forEachOrdered(p -> action.accept(p.getLeft(), p.getRight()));
  }

  @Override
  public @NotNull Object @NotNull [] toArray() {
    return stream.toArray();
  }

  @Override
  public @NotNull <A> A @NotNull [] toArray(IntFunction<A[]> generator) {
    return stream.toArray(generator);
  }

  @Override
  public Pair<T, T> reduce(Pair<T, T> identity, BinaryOperator<Pair<T, T>> accumulator) {
    return stream.reduce(identity, accumulator);
  }

  @Override
  public @NotNull Optional<Pair<T, T>> reduce(BinaryOperator<Pair<T, T>> accumulator) {
    return stream.reduce(accumulator);
  }

  @Override
  public <U> U reduce(U identity, BiFunction<U, ? super Pair<T, T>, U> accumulator, BinaryOperator<U> combiner) {
    return stream.reduce(identity, accumulator, combiner);
  }

  @Override
  public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super Pair<T, T>> accumulator, BiConsumer<R, R> combiner) {
    return stream.collect(supplier, accumulator, combiner);
  }

  @Override
  public <R, A> R collect(Collector<? super Pair<T, T>, A, R> collector) {
    return stream.collect(collector);
  }

  @Override
  public @NotNull Optional<Pair<T, T>> min(Comparator<? super Pair<T, T>> comparator) {
    return stream.min(comparator);
  }

  public @NotNull Optional<Pair<T, T>> min() {
    return min(Comparator.nullsFirst(Comparator.naturalOrder()));
  }

  public @NotNull Optional<Pair<T, T>> min(ToIntBiFunction<? super T, ? super T> comparator) {
    return min(Comparator.comparingInt(p -> comparator.applyAsInt(p.getLeft(), p.getRight())));
  }

  @Override
  public @NotNull Optional<Pair<T, T>> max(Comparator<? super Pair<T, T>> comparator) {
    return stream.max(comparator);
  }

  public @NotNull Optional<Pair<T, T>> max() {
    return max(Comparator.nullsLast(Comparator.naturalOrder()));
  }

  public @NotNull Optional<Pair<T, T>> max(ToIntBiFunction<? super T, ? super T> comparator) {
    return max(Comparator.comparingInt(p -> comparator.applyAsInt(p.getLeft(), p.getRight())));
  }

  @Override
  public long count() {
    return stream.count();
  }

  @Override
  public boolean anyMatch(Predicate<? super Pair<T, T>> predicate) {
    return stream.anyMatch(predicate);
  }

  public boolean anyMatchLeft(Predicate<? super T> predicate) {
    return map(Pair::getLeft).anyMatch(predicate);
  }

  public boolean anyMatchRight(Predicate<? super T> predicate) {
    return map(Pair::getRight).anyMatch(predicate);
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

  @Override
  public boolean allMatch(Predicate<? super Pair<T, T>> predicate) {
    return stream.allMatch(predicate);
  }

  public boolean allMatchLeft(Predicate<? super T> predicate) {
    return map(Pair::getLeft).allMatch(predicate);
  }

  public boolean allMatchRight(Predicate<? super T> predicate) {
    return map(Pair::getRight).allMatch(predicate);
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

  @Override
  public boolean noneMatch(Predicate<? super Pair<T, T>> predicate) {
    return stream.noneMatch(predicate);
  }

  public boolean noneMatchLeft(Predicate<? super T> predicate) {
    return map(Pair::getLeft).noneMatch(predicate);
  }

  public boolean noneMatchRight(Predicate<? super T> predicate) {
    return map(Pair::getRight).noneMatch(predicate);
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

  @Override
  public @NotNull Optional<Pair<T, T>> findFirst() {
    return stream.findFirst();
  }

  public @NotNull Pair<T, T> toPair() {
    return findFirst().orElseThrow();
  }

  @Override
  public @NotNull Optional<Pair<T, T>> findAny() {
    return stream.findAny();
  }

  @Override
  public @NotNull Iterator<Pair<T, T>> iterator() {
    return stream.iterator();
  }

  @Override
  public @NotNull Spliterator<Pair<T, T>> spliterator() {
    return stream.spliterator();
  }

  @Override
  public boolean isParallel() {
    return stream.isParallel();
  }

  @Override
  public @NotNull PairStream<T> sequential() {
    return isParallel() ? new PairStream<>(stream.sequential()) : this;
  }

  @Override
  public @NotNull PairStream<T> parallel() {
    return isParallel() ? this : new PairStream<>(stream.parallel());
  }

  @Override
  public @NotNull PairStream<T> unordered() {
    return new PairStream<>(stream.unordered());
  }

  @Override
  public @NotNull PairStream<T> onClose(@NotNull Runnable closeHandler) {
    return new PairStream<>(stream.onClose(closeHandler));
  }

  @Override
  public void close() {
    stream.close();
  }

}
