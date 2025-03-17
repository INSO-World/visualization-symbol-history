package com.mategka.dava.analyzer.extension.stream;

import com.mategka.dava.analyzer.extension.Covariant;
import com.mategka.dava.analyzer.extension.Pair;
import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.option.Options;

import com.google.common.collect.Streams;
import lombok.AccessLevel;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class AnStream<T> extends AbstractStreamAdapter<T, AnStream<T>> {

  private AnStream(Stream<T> stream) {
    super(stream, AnStream::new);
  }

  public static <T> AnStream<T> empty() {
    return new AnStream<>(Stream.empty());
  }

  public static <T> AnStream<T> from(T[] array) {
    return new AnStream<>(Arrays.stream(array));
  }

  @SafeVarargs
  public static <T> AnStream<T> from(T firstValue, T... values) {
    var result = new AnStream<>(Stream.of(firstValue));
    if (values.length > 0) result = result.concat(Stream.of(values));
    return result;
  }

  public static <T> AnStream<T> from(Collection<? extends T> collection) {
    return new AnStream<>(Covariant.stream(collection.stream()));
  }

  @SafeVarargs
  public static <T> AnStream<T> cons(T head, Stream<? extends T>... tails) {
    if (tails.length == 0) {
      return AnStream.from(head);
    }
    return AnStream.from(head).concat(AnStream.from(tails).flatMap(Function.identity()));
  }

  public static <T> AnStream<Pair<T, Integer>> fromIndexed(Collection<T> collection) {
    return new AnStream<>(AnStream.zip(collection.stream(), IntStream.range(0, collection.size()).boxed()));
  }

  public static <L, R> AnStream<Pair<L, R>> zip(Stream<L> streamA, Stream<R> streamB) {
    //noinspection UnstableApiUsage
    return new AnStream<>(Streams.zip(streamA, streamB, Pair::of));
  }

  public <U extends T> AnStream<U> allow(Class<U> clazz) {
    return filter(clazz).map(clazz::cast);
  }

  public AnStream<T> concat(Stream<? extends T> stream) {
    return new AnStream<>(Stream.concat(this.stream, stream));
  }

  @Override
  public <R> AnStream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
    return new AnStream<>(stream.flatMap(mapper));
  }

  @Override
  public <R> AnStream<R> map(Function<? super T, ? extends R> mapper) {
    return new AnStream<>(stream.map(mapper));
  }

  @Override
  public <R> AnStream<R> mapMulti(BiConsumer<? super T, ? super Consumer<R>> mapper) {
    return new AnStream<>(stream.mapMulti(mapper));
  }

  public <R> AnStream<R> mapOption(Function<? super T, Option<R>> mapper) {
    return map(mapper).mapMulti(Options.yieldIfSome());
  }

  public AnStream<T> reject(Class<? extends T> clazz) {
    return filter(Predicate.not(clazz::isInstance));
  }

}
