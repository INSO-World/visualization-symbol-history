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
public class Streamer<T> extends AbstractStreamAdapter<T, Streamer<T>> {

  private Streamer(Stream<T> stream) {
    super(stream, Streamer::new);
  }

  public static <T> Streamer<T> empty() {
    return new Streamer<>(Stream.empty());
  }

  public static <T> Streamer<T> ofCollection(Collection<T> collection) {
    return new Streamer<>(collection.stream());
  }

  public static <T> Streamer<Pair<T, Integer>> ofCollectionWithIndex(Collection<T> collection) {
    return new Streamer<>(Streamer.zip(collection.stream(), IntStream.range(0, collection.size()).boxed()));
  }

  public static <T> Streamer<T> ofArray(T[] array) {
    return new Streamer<>(Arrays.stream(array));
  }

  @SafeVarargs
  public static <T> Streamer<T> of(T... values) {
    return new Streamer<>(Stream.of(values));
  }

  @SafeVarargs
  public static <T> Streamer<T> cons(T head, Stream<? extends T>... tails) {
    if (tails.length == 0) {
      return Streamer.of(head);
    }
    return Streamer.of(head).concat(Streamer.ofArray(tails).flatMap(Function.identity()));
  }

  public static <L, R> Streamer<Pair<L, R>> zip(Stream<L> streamA, Stream<R> streamB) {
    //noinspection UnstableApiUsage
    return new Streamer<>(Streams.zip(streamA, streamB, Pair::of));
  }

  public Stepper<T> stepper() {
    return new Stepper<>(stream.spliterator());
  }

  public Streamer<T> concat(Stream<? extends T> stream) {
    return new Streamer<>(Stream.concat(this.stream, stream));
  }

  @Override
  public <R> Streamer<R> map(Function<? super T, ? extends R> mapper) {
    return new Streamer<>(stream.map(mapper));
  }

  @Override
  public <R> Streamer<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
    return new Streamer<>(stream.flatMap(mapper));
  }

  @Override
  public <R> Streamer<R> mapMulti(BiConsumer<? super T, ? super Consumer<R>> mapper) {
    return new Streamer<>(stream.mapMulti(mapper));
  }

  public <U extends T> Streamer<U> narrow(Class<U> clazz) {
    return new Streamer<>(stream.map(e -> Option.cast(e, clazz)).mapMulti(Option.yieldIfSome()));
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
