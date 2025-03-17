package com.mategka.dava.analyzer.extension.option;

import com.mategka.dava.analyzer.extension.ComparatorsX;

import com.mategka.dava.analyzer.extension.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.*;

public sealed interface Option<T> extends Comparable<Option<T>> permits None, Some {

  static <T> @NotNull Option<T> None() {
    return None.instance();
  }

  static <T> @NotNull Option<T> Some(@NotNull T value) {
    return Some.of(value);
  }

  static <L, R> @NotNull Option<Pair<L, R>> flatten(@NotNull Pair<Option<L>, Option<R>> optionPair) {
    return pair(optionPair.left(), optionPair.right());
  }

  static <L, R> @NotNull Option<Pair<L, R>> pair(@NotNull L left, @NotNull R right) {
    return Some(Pair.of(left, right));
  }

  static <L, R> @NotNull Option<Pair<L, R>> pair(@NotNull Option<L> left, @NotNull Option<R> right) {
    if (left.isSome() && right.isSome()) {
      return Some(Pair.of(left.getOrThrow(), right.getOrThrow()));
    }
    return None();
  }

  boolean contains(@Nullable T value);

  @NotNull Option<T> filter(@NotNull Predicate<T> predicate);

  default <U> @NotNull Option<U> flatMap(@NotNull Function<T, Option<U>> mapper) {
    return fold(mapper, Option::None);
  }

  <U> U fold(@NotNull Function<T, U> ifSome, @NotNull Supplier<U> ifNone);

  default T getOrCompute(@NotNull Supplier<T> supplier) {
    return fold(Function.identity(), supplier);
  }

  default T getOrElse(T defaultValue) {
    return getOrCompute(() -> defaultValue);
  }

  default T getOrNull() {
    return getOrElse(null);
  }

  default @NotNull T getOrThrow() {
    return getOrThrow(() -> new NoSuchElementException("Option was None"));
  }

  @NotNull T getOrThrow(@NotNull Supplier<? extends RuntimeException> exceptionSupplier);

  void ifNone(@NotNull Runnable runnable);

  void ifSome(@NotNull Consumer<T> consumer);

  boolean isNone();

  boolean isSome();

  default <U> @NotNull Option<U> map(@NotNull Function<T, U> mapper) {
    return fold(t -> Options.fromNullable(mapper.apply(t)), Option::None);
  }

  <U extends T> @NotNull Option<U> narrow(@NotNull Class<U> clazz);

  @NotNull Option<T> or(@NotNull Supplier<Option<T>> alternative);

  default @NotNull Optional<T> toOptional() {
    return fold(Optional::of, Optional::empty);
  }

  @Override
  default int compareTo(@NotNull Option<T> o) {
    return fold(
      value -> o instanceof Some<T> some ? ComparatorsX.compare(value, some.value()) : -1,
      () -> o instanceof Some<T> ? 1 : 0
    );
  }

}
