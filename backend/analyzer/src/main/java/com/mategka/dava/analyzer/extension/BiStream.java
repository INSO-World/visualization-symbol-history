package com.mategka.dava.analyzer.extension;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BiStream<T> {

  Stream<T> stream;

  public static <T> BiStream<T> of(T left, T right) {
    return new BiStream<>(Stream.of(left, right));
  }

  public static <T> BiStream<T> of(Pair<? extends T, ? extends T> pair) {
    return of(pair.getLeft(), pair.getRight());
  }

  public Pair<T, T> toPair() {
    return collect(CollectorsX.toPair());
  }

  public <R> R collect(BiFunction<T, T, R> combiner) {
    var pair = toPair();
    return combiner.apply(pair.getLeft(), pair.getRight());
  }

  public Stream<T> toElementStream() {
    return stream;
  }

  public Stream<Pair<T, T>> toPairStream() {
    return Stream.of(toPair());
  }

  public BiStream<Optional<T>> filter(Predicate<? super T> predicate) {
    return map(e -> Optional.ofNullable(e).filter(predicate));
  }

  public BiStream<Optional<T>> nonNull() {
    return map(Optional::ofNullable);
  }

  public <R> BiStream<R> map(Function<? super T, ? extends R> mapper) {
    return new BiStream<>(stream.map(mapper));
  }

  public BiStream<T> mapLeft(Function<? super T, ? extends T> mapper) {
    return new BiStream<>(toPairStream().flatMap(p -> Stream.of(mapper.apply(p.getLeft()), p.getRight())));
  }

  public BiStream<T> mapRight(Function<? super T, ? extends T> mapper) {
    return new BiStream<>(toPairStream().flatMap(p -> Stream.of(p.getLeft(), mapper.apply(p.getRight()))));
  }

  public BiStream<T> flipped() {
    return new BiStream<>(toPairStream().flatMap(p -> Stream.of(p.getRight(), p.getLeft())));
  }

  public BiStream<T> sorted() {
    return new BiStream<>(stream.sorted());
  }

  public BiStream<T> sorted(Comparator<? super T> comparator) {
    return new BiStream<>(stream.sorted(comparator));
  }

  public BiStream<T> peek(Consumer<? super T> action) {
    return new BiStream<>(stream.peek(action));
  }

  public void forEach(Consumer<? super T> action) {
    stream.forEachOrdered(action);
  }

  public @NotNull Object[] toArray() {
    return stream.toArray();
  }

  public @NotNull <A> A[] toArray(IntFunction<A[]> generator) {
    return stream.toArray(generator);
  }

  public T reduce(T identity, BinaryOperator<T> accumulator) {
    return stream.reduce(identity, accumulator);
  }

  public @NotNull Optional<T> reduce(BinaryOperator<T> accumulator) {
    return stream.reduce(accumulator);
  }

  public <R, A> R collect(Collector<? super T, A, R> collector) {
    return stream.collect(collector);
  }

  public @NotNull Optional<T> min(Comparator<? super T> comparator) {
    return stream.min(comparator);
  }

  public @NotNull Optional<T> max(Comparator<? super T> comparator) {
    return stream.max(comparator);
  }

  public long count() {
    return 2;
  }

  public boolean anyMatch(Predicate<? super T> predicate) {
    return stream.anyMatch(predicate);
  }

  public boolean allMatch(Predicate<? super T> predicate) {
    return stream.allMatch(predicate);
  }

  public boolean noneMatch(Predicate<? super T> predicate) {
    return stream.noneMatch(predicate);
  }

  public T getLeft() {
    return stream.findFirst().orElseThrow();
  }

  public T getRight() {
    return stream.skip(1).findFirst().orElseThrow();
  }

  public @NotNull Iterator<T> iterator() {
    return stream.iterator();
  }

  public @NotNull Spliterator<T> spliterator() {
    return stream.spliterator();
  }

}
