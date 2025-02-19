package com.mategka.dava.analyzer.extension;

import com.mategka.dava.analyzer.extension.option.Option;

import com.google.common.collect.Streams;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;
import java.util.Collection;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class MyStream<T> extends AbstractStreamAdapter<T, MyStream<T>> {

  private MyStream(Stream<T> stream) {
    super(stream, MyStream::new);
  }

  public static <T> MyStream<T> empty() {
    return new MyStream<>(Stream.empty());
  }

  public static <T> MyStream<T> from(Collection<T> collection) {
    return new MyStream<>(collection.stream());
  }

  public static <T> MyStream<Pair<T, Integer>> fromIndexed(Collection<T> collection) {
    return new MyStream<>(MyStream.zip(collection.stream(), IntStream.range(0, collection.size()).boxed()));
  }

  public static <T> MyStream<T> from(T[] array) {
    return new MyStream<>(Arrays.stream(array));
  }

  @SafeVarargs
  public static <T> MyStream<T> from(T firstValue, T... values) {
    var result = new MyStream<>(Stream.of(firstValue));
    if (values.length > 0) result = result.concat(Stream.of(values));
    return result;
  }

  @SafeVarargs
  public static <T> MyStream<T> cons(T head, Stream<? extends T>... tails) {
    if (tails.length == 0) {
      return MyStream.from(head);
    }
    return MyStream.from(head).concat(MyStream.from(tails).flatMap(Function.identity()));
  }

  public static <L, R> MyStream<Pair<L, R>> zip(Stream<L> streamA, Stream<R> streamB) {
    //noinspection UnstableApiUsage
    return new MyStream<>(Streams.zip(streamA, streamB, Pair::of));
  }

  public Stepper<T> stepper() {
    return new Stepper<>(stream.spliterator());
  }

  public MyStream<T> concat(Stream<? extends T> stream) {
    return new MyStream<>(Stream.concat(this.stream, stream));
  }

  @Override
  public <R> MyStream<R> map(Function<? super T, ? extends R> mapper) {
    return new MyStream<>(stream.map(mapper));
  }

  @Override
  public <R> MyStream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
    return new MyStream<>(stream.flatMap(mapper));
  }

  @Override
  public <R> MyStream<R> mapMulti(BiConsumer<? super T, ? super Consumer<R>> mapper) {
    return new MyStream<>(stream.mapMulti(mapper));
  }

  public <U extends T> MyStream<U> narrow(Class<U> clazz) {
    return new MyStream<>(stream.map(e -> Option.cast(e, clazz)).mapMulti(Option.yieldIfSome()));
  }

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Stepper<T> {

    private final Spliterator<T> spliterator;

    public Stepper<T> takeOne(Consumer<? super T> consumer) {
      spliterator.tryAdvance(consumer);
      return this;
    }

    public void forEachRemaining(Consumer<? super T> consumer) {
      spliterator.forEachRemaining(consumer);
    }

  }

}
