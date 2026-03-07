package com.mategka.dava.analyzer.extension.option;

import com.mategka.dava.analyzer.extension.struct.Pair;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@UtilityClass
public class Options {

  public <T, U extends T> @NotNull Option<U> cast(@Nullable T value, @NotNull Class<U> clazz) {
    return fromNullable(value).narrow(clazz);
  }

  public <L, R> @NotNull Option<Pair<L, R>> flatten(@NotNull Pair<Option<L>, Option<R>> optionPair) {
    return pair(optionPair.left(), optionPair.right());
  }

  public <T> @NotNull Option<T> fromCallable(@NotNull Callable<T> callable) {
    try {
      return fromNullable(callable.call());
    } catch (Exception _e) {
      return Option.None();
    }
  }

  public <T> @NotNull Option<T> fromNullable(@Nullable T value) {
    return (value != null) ? Option.Some(value) : Option.None();
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public <T> @NotNull Option<T> fromOptional(@NotNull Optional<T> optional) {
    return optional.map(Option::Some).orElseGet(Option::None);
  }

  public <T extends Collection<?>> @NotNull Option<T> fromSized(@NotNull T value) {
    return Options.when(!value.isEmpty(), () -> value);
  }

  public <T> @NotNull Option<T> getFirst(@NotNull SequencedCollection<? extends T> collection) {
    return when(!collection.isEmpty(), collection::getFirst);
  }

  public <T> @NotNull Option<T> getFirst(@NotNull Iterable<? extends T> iterable) {
    var iterator = iterable.iterator();
    return when(iterator.hasNext(), iterator::next);
  }

  public <T> @NotNull Option<T> getFirst(@NotNull T[] array) {
    return when(array.length > 0, () -> array[0]);
  }

  public <L, R> @NotNull Option<Pair<L, R>> pair(@NotNull L left, @NotNull R right) {
    return Option.Some(Pair.of(left, right));
  }

  public <L, R> @NotNull Option<Pair<L, R>> pair(@NotNull Option<L> left, @NotNull Option<R> right) {
    if (left.isSome() && right.isSome()) {
      return Option.Some(Pair.of(left.getOrThrow(), right.getOrThrow()));
    }
    return Option.None();
  }

  public <T> @NotNull Option<T> when(boolean condition, @NotNull Supplier<T> supplier) {
    return condition ? fromNullable(supplier.get()) : Option.None();
  }

  public <T> BiConsumer<Option<T>, Consumer<T>> yieldIfSome() {
    return Option::ifSome;
  }

}
