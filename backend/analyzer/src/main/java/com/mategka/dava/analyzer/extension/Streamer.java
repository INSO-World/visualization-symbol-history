package com.mategka.dava.analyzer.extension;

import lombok.AccessLevel;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class Streamer<T> extends AbstractStreamAdapter<T, Streamer<T>> {

  private Streamer(Stream<T> stream) {
    super(stream, Streamer::new);
  }

  public static <T> Streamer<T> of(Collection<T> collection) {
    return new Streamer<>(collection.stream());
  }

  public static <T> Streamer<T> of(T[] array) {
    return new Streamer<>(Arrays.stream(array));
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

}
