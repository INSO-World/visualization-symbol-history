package com.mategka.dava.analyzer.extension.option;

import com.mategka.dava.analyzer.extension.ComparatorsX;
import com.mategka.dava.analyzer.extension.Pair;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.concurrent.Callable;
import java.util.function.*;

public sealed interface Option<T> extends Comparable<Option<T>> permits None, Some {

  static <T> @NotNull Option<T> Some(@NonNull T value) {
    return new Some<>(value);
  }

  static <T> @NotNull Option<T> None() {
    return None.instance();
  }

  static <T, U extends T> @NotNull Option<U> cast(@Nullable T value, @NotNull Class<U> clazz) {
    return fromNullable(value).narrow(clazz);
  }

  static <T> @NotNull Option<T> when(boolean condition, @NotNull Supplier<T> supplier) {
    return condition ? fromNullable(supplier.get()) : None();
  }

  static <T> @NotNull Option<T> getFirst(@NotNull SequencedCollection<? extends T> collection) {
    return when(!collection.isEmpty(), collection::getFirst);
  }

  static <T> @NotNull Option<T> getFirst(@NotNull Iterable<? extends T> iterable) {
    var iterator = iterable.iterator();
    return when(iterator.hasNext(), iterator::next);
  }

  static <T> @NotNull Option<T> getFirst(@NotNull T[] array) {
    return when(array.length > 0, () -> array[0]);
  }

  static <L, R> @NotNull Option<Pair<L, R>> pair(@NotNull Option<L> left, @NotNull Option<R> right) {
    if (left.isSome() && right.isSome()) {
      return Some(Pair.of(left.getOrThrow(), right.getOrThrow()));
    }
    return None();
  }

  static <L, R> @NotNull Option<Pair<L, R>> pair(@NotNull Pair<Option<L>, Option<R>> optionPair) {
    return pair(optionPair.left(), optionPair.right());
  }

  static <T> BiConsumer<Option<T>, Consumer<T>> yieldIfSome() {
    return Option::ifSome;
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  static <T> @NotNull Option<T> fromOptional(@NotNull Optional<T> optional) {
    return optional.map(Option::Some).orElseGet(Option::None);
  }

  static <T> @NotNull Option<T> fromNullable(@Nullable T value) {
    return (value != null) ? Some(value) : None();
  }

  static <T> @NotNull Option<T> fromCallable(@NotNull Callable<T> callable) {
    try {
      return fromNullable(callable.call());
    } catch (Exception _e) {
      return None();
    }
  }

  boolean isSome();

  boolean isNone();

  default @NotNull T getOrThrow() {
    return getOrThrow(() -> new NoSuchElementException("Option was None"));
  }

  <E extends RuntimeException> @NotNull T getOrThrow(@NotNull Supplier<E> exceptionSupplier);

  default T getOrElse(T defaultValue) {
    return fold(Function.identity(), () -> defaultValue);
  }

  default T getOrCompute(@NotNull Supplier<T> supplier) {
    return fold(Function.identity(), supplier);
  }

  default T getOrNull() {
    return getOrElse(null);
  }

  void ifSome(@NotNull Consumer<T> consumer);

  void ifNone(@NotNull Runnable runnable);

  default @NotNull Optional<T> toOptional() {
    return fold(Optional::of, Optional::empty);
  }

  boolean contains(@Nullable T value);

  <U> U fold(@NotNull Function<T, U> ifSome, @NotNull Supplier<U> ifNone);

  default <U> @NotNull Option<U> map(@NotNull Function<T, U> mapper) {
    return fold(t -> fromNullable(mapper.apply(t)), Option::None);
  }

  <U extends T> @NotNull Option<U> narrow(@NotNull Class<U> clazz);

  @NotNull Option<T> or(@NotNull Supplier<Option<T>> alternative);

  @NotNull Option<T> filter(@NotNull Predicate<T> predicate);

  default <U> @NotNull Option<U> flatMap(@NotNull Function<T, Option<U>> mapper) {
    return fold(mapper, Option::None);
  }

  @Override
  default int compareTo(@NotNull Option<T> o) {
    return fold(
      value -> o instanceof Some<T> some ? ComparatorsX.compare(value, some.value()) : -1,
      () -> o instanceof Some<T> ? 1 : 0
    );
  }

}
