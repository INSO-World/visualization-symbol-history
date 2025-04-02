package com.mategka.dava.analyzer.extension.stream;

import com.mategka.dava.analyzer.extension.Covariant;
import com.mategka.dava.analyzer.extension.Pair;
import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.option.Options;

import com.google.common.collect.Streams;
import lombok.AccessLevel;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.util.*;
import java.util.function.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

  public static <T> AnStream<T> iterate(Supplier<Boolean> hasNext, Supplier<T> next) {
    return new AnStream<>(Stream.<T>iterate(null, _t -> hasNext.get(), _t -> next.get()).skip(1));
  }

  public static <T> AnStream<T> singleton(T value) {
    return new AnStream<>(Stream.of(value));
  }

  @SafeVarargs
  public static <T> AnStream<T> sequence(T... values) {
    return new AnStream<>(Stream.of(values));
  }

  public static <T> AnStream<T> from(Spliterator<? extends T> spliterator) {
    var parallel = spliterator.hasCharacteristics(Spliterator.CONCURRENT);
    return new AnStream<>(Covariant.stream(StreamSupport.stream(spliterator, parallel)));
  }

  public static <T> AnStream<T> from(Collection<? extends T> collection) {
    return new AnStream<>(Covariant.stream(collection.stream()));
  }

  public static <K, V> AnStream<Map.Entry<K, V>> from(Map<K, V> map) {
    return new AnStream<>(map.entrySet().stream());
  }

  @SafeVarargs
  public static <T> AnStream<T> cons(T head, Stream<? extends T>... tails) {
    if (tails.length == 0) {
      return AnStream.singleton(head);
    }
    return AnStream.singleton(head).concat(AnStream.from(tails).flatMap(Function.identity()));
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
