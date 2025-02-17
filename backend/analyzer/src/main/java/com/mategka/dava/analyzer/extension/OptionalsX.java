package com.mategka.dava.analyzer.extension;

import com.leakyabstractions.result.core.Results;
import lombok.experimental.UtilityClass;

import java.util.Optional;
import java.util.SequencedCollection;
import java.util.concurrent.Callable;
import java.util.function.*;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@UtilityClass
public class OptionalsX {

  public <T> Optional<T> cast(Optional<? super T> optional, Class<T> targetType) {
    return optional.filter(targetType::isInstance).map(targetType::cast);
  }

  public <T, U extends T> Optional<U> cast(T value, Class<U> targetType) {
    return cast(Optional.ofNullable(value), targetType);
  }

  public <T, U> Optional<U> map(T value, Function<T, U> mapperFn) {
    return Optional.ofNullable(mapperFn.apply(value));
  }

  public <T> Optional<T> when(boolean condition, Supplier<T> supplier) {
    return condition ? Optional.of(supplier.get()) : Optional.empty();
  }

  public <T> Optional<T> getFirst(SequencedCollection<? extends T> collection) {
    return when(!collection.isEmpty(), collection::getFirst);
  }

  public <T> Optional<T> getFirst(Iterable<? extends T> iterable) {
    var iterator = iterable.iterator();
    return when(iterator.hasNext(), iterator::next);
  }

  public <T> Optional<T> getFirst(T[] array) {
    return when(array.length > 0, () -> array[0]);
  }

  public <T> Optional<T> ofCallable(Callable<? extends T> callable) {
    return Results.<T>ofCallable(callable).getSuccess();
  }

  public <L, R> Optional<Pair<L, R>> pair(Optional<L> left, Optional<R> right) {
    if (left.isPresent() && right.isPresent()) {
      return Optional.of(Pair.of(left.get(), right.get()));
    }
    return Optional.empty();
  }

  public <L, R> Optional<Pair<L, R>> pair(Pair<Optional<L>, Optional<R>> optionalPair) {
    return pair(optionalPair.left(), optionalPair.right());
  }

  public <T> BiConsumer<Optional<T>, Consumer<T>> yieldIfPresent() {
    return Optional::ifPresent;
  }

}
