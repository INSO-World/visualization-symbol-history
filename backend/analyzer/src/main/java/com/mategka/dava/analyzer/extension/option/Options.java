package com.mategka.dava.analyzer.extension.option;

import com.mategka.dava.analyzer.extension.Pair;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.SequencedCollection;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@UtilityClass
public class Options {

  public static <T, U extends T> @NotNull Option<U> cast(@Nullable T value, @NotNull Class<U> clazz) {
    return fromNullable(value).narrow(clazz);
  }

  public static <T> @NotNull Option<T> fromCallable(@NotNull Callable<T> callable) {
    try {
      return fromNullable(callable.call());
    } catch (Exception _e) {
      return Option.None();
    }
  }

  public static <T> @NotNull Option<T> fromNullable(@Nullable T value) {
    return (value != null) ? Option.Some(value) : Option.None();
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static <T> @NotNull Option<T> fromOptional(@NotNull Optional<T> optional) {
    return optional.map(Option::Some).orElseGet(Option::None);
  }

  public static <T> @NotNull Option<T> getFirst(@NotNull SequencedCollection<? extends T> collection) {
    return when(!collection.isEmpty(), collection::getFirst);
  }

  public static <T> @NotNull Option<T> getFirst(@NotNull Iterable<? extends T> iterable) {
    var iterator = iterable.iterator();
    return when(iterator.hasNext(), iterator::next);
  }

  public static <T> @NotNull Option<T> getFirst(@NotNull T[] array) {
    return when(array.length > 0, () -> array[0]);
  }

  public static <L, R> @NotNull Option<Pair<L, R>> pair(@NotNull Option<L> left, @NotNull Option<R> right) {
    if (left.isSome() && right.isSome()) {
      return Option.Some(Pair.of(left.getOrThrow(), right.getOrThrow()));
    }
    return Option.None();
  }

  public static <L, R> @NotNull Option<Pair<L, R>> pair(@NotNull Pair<Option<L>, Option<R>> optionPair) {
    return pair(optionPair.left(), optionPair.right());
  }

  public static <T> @NotNull Option<T> when(boolean condition, @NotNull Supplier<T> supplier) {
    return condition ? fromNullable(supplier.get()) : Option.None();
  }

  public static <T> BiConsumer<Option<T>, Consumer<T>> yieldIfSome() {
    return Option::ifSome;
  }

}
