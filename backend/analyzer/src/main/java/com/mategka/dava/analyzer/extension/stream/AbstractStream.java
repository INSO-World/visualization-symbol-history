package com.mategka.dava.analyzer.extension.stream;

import com.mategka.dava.analyzer.extension.option.Option;

import com.mategka.dava.analyzer.extension.option.Options;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractStream<T> implements Stream<T> {

  protected final Stream<T> stream;

  @Override
  public boolean allMatch(Predicate<? super T> predicate) {
    return stream.allMatch(predicate);
  }

  @Override
  public boolean anyMatch(Predicate<? super T> predicate) {
    return stream.anyMatch(predicate);
  }

  @Override
  public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
    return stream.collect(supplier, accumulator, combiner);
  }

  @Override
  public <R, A> R collect(Collector<? super T, A, R> collector) {
    return stream.collect(collector);
  }

  @Override
  public long count() {
    return stream.count();
  }

  @Override
  public @NotNull Optional<T> findAny() {
    return stream.findAny();
  }

  public @NotNull Option<T> findAnyAsOption() {
    return Options.fromOptional(findAny());
  }

  @Override
  public @NotNull Optional<T> findFirst() {
    return stream.findFirst();
  }

  public @NotNull Option<T> findFirstAsOption() {
    return Options.fromOptional(findFirst());
  }

  @Override
  public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
    return stream.flatMapToDouble(mapper);
  }

  @Override
  public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
    return stream.flatMapToInt(mapper);
  }

  @Override
  public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
    return stream.flatMapToLong(mapper);
  }

  @Override
  public void forEach(Consumer<? super T> action) {
    stream.forEach(action);
  }

  @Override
  public void forEachOrdered(Consumer<? super T> action) {
    stream.forEachOrdered(action);
  }

  @Override
  public boolean isParallel() {
    return stream.isParallel();
  }

  public @NotNull Iterable<T> iterable() {
    return new Iterable<>() {
      @Override
      public @NotNull Iterator<T> iterator() {
        return stream.iterator();
      }
    };
  }

  @Override
  public @NotNull Iterator<T> iterator() {
    return stream.iterator();
  }

  @Override
  public DoubleStream mapMultiToDouble(BiConsumer<? super T, ? super DoubleConsumer> mapper) {
    return stream.mapMultiToDouble(mapper);
  }

  @Override
  public IntStream mapMultiToInt(BiConsumer<? super T, ? super IntConsumer> mapper) {
    return stream.mapMultiToInt(mapper);
  }

  @Override
  public LongStream mapMultiToLong(BiConsumer<? super T, ? super LongConsumer> mapper) {
    return stream.mapMultiToLong(mapper);
  }

  @Override
  public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
    return stream.mapToDouble(mapper);
  }

  @Override
  public IntStream mapToInt(ToIntFunction<? super T> mapper) {
    return stream.mapToInt(mapper);
  }

  @Override
  public LongStream mapToLong(ToLongFunction<? super T> mapper) {
    return stream.mapToLong(mapper);
  }

  @Override
  public @NotNull Optional<T> max(Comparator<? super T> comparator) {
    return stream.max(comparator);
  }

  public @NotNull Option<T> maxAsOption(Comparator<? super T> comparator) {
    return Options.fromOptional(max(comparator));
  }

  @Override
  public @NotNull Optional<T> min(Comparator<? super T> comparator) {
    return stream.min(comparator);
  }

  public @NotNull Option<T> minAsOption(Comparator<? super T> comparator) {
    return Options.fromOptional(min(comparator));
  }

  @Override
  public boolean noneMatch(Predicate<? super T> predicate) {
    return stream.noneMatch(predicate);
  }

  @Override
  public T reduce(T identity, BinaryOperator<T> accumulator) {
    return stream.reduce(identity, accumulator);
  }

  @Override
  public @NotNull Optional<T> reduce(BinaryOperator<T> accumulator) {
    return stream.reduce(accumulator);
  }

  @Override
  public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
    return stream.reduce(identity, accumulator, combiner);
  }

  public @NotNull Option<T> reduceAsOption(BinaryOperator<T> accumulator) {
    return Options.fromOptional(reduce(accumulator));
  }

  @Override
  public @NotNull Spliterator<T> spliterator() {
    return stream.spliterator();
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
  public List<T> toList() {
    return stream.toList();
  }

  public List<T> toList(Supplier<? extends List<T>> supplier) {
    return stream.collect(Collectors.toCollection(supplier));
  }

  public List<T> toMutableList() {
    return toList(ArrayList::new);
  }

  @Override
  public void close() {
    stream.close();
  }

  @Override
  public boolean equals(Object o) {
    return switch (o) {
      case AbstractStream<?> that -> stream.equals(that.stream);
      case Stream<?> that -> stream.equals(that);
      default -> false;
    };
  }

  @Override
  public int hashCode() {
    return stream.hashCode();
  }

}
