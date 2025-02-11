package com.mategka.dava.analyzer.extension;

import com.google.common.collect.Streams;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@UtilityClass
public class StreamsX {

  public Stream<String> splitting(String string, @NotNull String delimiter) {
    if (string == null) {
      return Stream.empty();
    }
    return Stream.of(string.split(delimiter));
  }

  public Stream<String> splitting(String string, @NotNull String delimiter, int limit) {
    if (string == null) {
      return Stream.empty();
    }
    return Stream.of(string.split(delimiter, limit));
  }

  public <K, V1, V2> Function<Map.Entry<K, V1>, Map.Entry<K, V2>> mappingValue(
    Function<? super V1, ? extends V2> mapper) {
    return entry -> Map.entry(entry.getKey(), mapper.apply(entry.getValue()));
  }

  public <T> Comparator<T> falseFirst(Predicate<T> keyMapper) {
    return Comparator.comparingInt(t -> keyMapper.test(t) ? 0 : -1);
  }

  public <T> Comparator<T> trueFirst(Predicate<T> keyMapper) {
    return Comparator.comparingInt(t -> keyMapper.test(t) ? -1 : 0);
  }

  @SafeVarargs
  public <T> Stream<T> cons(T head, Stream<? extends T>... tails) {
    if (tails.length == 0) {
      return Stream.of(head);
    }
    // NOTE: Type argument has to be present, otherwise Java seems to run into parsing errors?
    //noinspection RedundantTypeArguments
    return Stream.<T>concat(
      Stream.of(head),
      Arrays.stream(tails).flatMap(Function.identity())
    );
  }

  public <T, U> BiConsumer<T, Consumer<U>> mapToZeroOrOne(Function<T, Optional<U>> mapper) {
    return (v, c) -> mapper.apply(v).ifPresent(c);
  }

  public <T, U extends T> BiConsumer<T, Consumer<U>> onlyOfType(Class<? extends U> clazz) {
    return (v, c) -> {
      if (clazz.isInstance(v)) {
        c.accept(clazz.cast(v));
      }
    };
  }

  public <T> Stream<T> reverse(Stream<T> stream) {
    return stream.toList().reversed().stream();
  }

  public <L, R> Stream<Pair<L, R>> zip(Stream<L> streamA, Stream<R> streamB) {
    //noinspection UnstableApiUsage
    return Streams.zip(streamA, streamB, Pair::of);
  }

  @SafeVarargs
  public <T> Stream<T> concat(Stream<? extends T>... streams) {
    return Arrays.stream(streams).flatMap(Function.identity());
  }

  public <T> Stream<Pair<T, Integer>> streamWithIndex(Collection<T> collection) {
    return StreamsX.zip(collection.stream(), IntStream.range(0, collection.size()).boxed());
  }

  public <T> Stepper<T> stepper(Stream<T> stream) {
    return new Stepper<>(stream.spliterator());
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
