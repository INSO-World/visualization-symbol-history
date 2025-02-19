package com.mategka.dava.analyzer.extension.option;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PACKAGE, staticName = "of")
public final class Some<T> implements Option<T> {

  @NonNull
  T value;

  public @NotNull T value() {
    return value;
  }

  @Override
  public boolean isSome() {
    return true;
  }

  @Override
  public boolean isNone() {
    return false;
  }

  @Override
  public @NotNull T getOrThrow(@NotNull Supplier<? extends RuntimeException> exceptionSupplier) {
    return value;
  }

  @Override
  public void ifSome(@NotNull Consumer<T> consumer) {
    consumer.accept(value);
  }

  @Override
  public void ifNone(@NotNull Runnable _runnable) {
    // Do nothing
  }

  @Override
  public boolean contains(@Nullable T value) {
    return this.value.equals(value);
  }

  @Override
  public <U> U fold(@NotNull Function<T, U> ifSome, @NotNull Supplier<U> _ifNone) {
    return ifSome.apply(value);
  }

  @Override
  public <U extends T> @NotNull Option<U> narrow(@NotNull Class<U> clazz) {
    return filter(clazz::isInstance).map(clazz::cast);
  }

  @Override
  public @NotNull Option<T> or(@NotNull Supplier<Option<T>> _alternative) {
    return this;
  }

  @Override
  public @NotNull Option<T> filter(@NotNull Predicate<T> predicate) {
    return predicate.test(value) ? this : None.instance();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Some<?> some)) return false;
    return value.equals(some.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

}
