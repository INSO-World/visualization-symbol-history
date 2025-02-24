package com.mategka.dava.analyzer.extension;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class AbstractStreamAdapter<T, S extends AbstractStreamAdapter<T, S>> extends AbstractStream<T> {

  private final Function<Stream<T>, S> sameKindMapper;

  protected AbstractStreamAdapter(Stream<T> stream, Function<Stream<T>, S> sameKindMapper) {
    super(stream);
    this.sameKindMapper = sameKindMapper;
  }

  @Override
  public S distinct() {
    return sameKindMapper.apply(stream.distinct());
  }

  @Override
  public S dropWhile(Predicate<? super T> predicate) {
    return sameKindMapper.apply(stream.dropWhile(predicate));
  }

  @Override
  public S filter(Predicate<? super T> predicate) {
    return sameKindMapper.apply(stream.filter(predicate));
  }

  @Override
  public S limit(long maxSize) {
    return sameKindMapper.apply(stream.limit(maxSize));
  }

  @Override
  public @NotNull S onClose(@NotNull Runnable closeHandler) {
    return sameKindMapper.apply(stream.onClose(closeHandler));
  }

  @Override
  public @NotNull S parallel() {
    return sameKindMapper.apply(stream.parallel());
  }

  @Override
  public S peek(Consumer<? super T> action) {
    return sameKindMapper.apply(stream.peek(action));
  }

  @Override
  public @NotNull S sequential() {
    return sameKindMapper.apply(stream.sequential());
  }

  @Override
  public S skip(long n) {
    return sameKindMapper.apply(stream.skip(n));
  }

  @Override
  public S sorted() {
    return sameKindMapper.apply(stream.sorted());
  }

  @Override
  public S sorted(Comparator<? super T> comparator) {
    return sameKindMapper.apply(stream.sorted(comparator));
  }

  @Override
  public S takeWhile(Predicate<? super T> predicate) {
    return sameKindMapper.apply(stream.takeWhile(predicate));
  }

  @Override
  public @NotNull S unordered() {
    return sameKindMapper.apply(stream.unordered());
  }

}
