package com.mategka.dava.analyzer.extension.option;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class None<T> implements Option<T> {

  @SuppressWarnings("rawtypes")
  private static final None INSTANCE = new None();

  static <T> None<T> instance() {
    //noinspection unchecked
    return None.INSTANCE;
  }

  @Override
  public boolean isSome() {
    return false;
  }

  @Override
  public boolean isNone() {
    return true;
  }

  @Override
  public <E extends RuntimeException> @NotNull T getOrThrow(@NotNull Supplier<E> exceptionSupplier) {
    throw exceptionSupplier.get();
  }

  @Override
  public void ifSome(@NotNull Consumer<T> _consumer) {
    // Do nothing
  }

  @Override
  public void ifNone(@NotNull Runnable runnable) {
    runnable.run();
  }

  @Override
  public boolean contains(@Nullable T value) {
    return false;
  }

  @Override
  public <U> U fold(@NotNull Function<T, U> _ifSome, @NotNull Supplier<U> ifNone) {
    return ifNone.get();
  }

  @Override
  public @NotNull <U extends T> Option<U> narrow(@NotNull Class<U> _clazz) {
    return instance();
  }

  @Override
  public @NotNull Option<T> or(@NotNull Supplier<Option<T>> alternative) {
    return alternative.get();
  }

  @Override
  public @NotNull Option<T> filter(@NotNull Predicate<T> _predicate) {
    return this;
  }

}
