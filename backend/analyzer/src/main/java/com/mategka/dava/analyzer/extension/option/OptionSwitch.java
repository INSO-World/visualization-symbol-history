package com.mategka.dava.analyzer.extension.option;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class OptionSwitch<T, R> {

  final Option<T> option;
  Option<@NotNull R> result = Option.None();

  public OptionSwitch<T, R> someSpecific(Predicate<T> predicate, Function<T, @NotNull R> mapper) {
    result = result.or(() -> option.filter(predicate).map(mapper));
    return this;
  }

  public OptionSwitch<T, R> some(Function<T, @NotNull R> mapper) {
    result = result.or(() -> option.map(mapper));
    return this;
  }

  public OptionSwitch<T, R> none(Supplier<@NotNull R> supplier) {
    result = result.or(() -> Option.Some(supplier.get()));
    return this;
  }

  @UnknownNullability
  public R resolve() throws NoSuchElementException {
    return result.getOrNull();
  }

}
