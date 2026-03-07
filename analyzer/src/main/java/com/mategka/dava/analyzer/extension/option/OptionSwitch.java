package com.mategka.dava.analyzer.extension.option;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

sealed interface OptionSwitch {

  @Contract("_ -> new")
  static <T> OptionSwitch.@NotNull SomeStage<T> of(@NotNull Option<T> option) {
    return new SomeStage<>(option);
  }

  @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
  @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
  final class SomeStage<T> implements OptionSwitch {

    Option<T> option;

    @Contract("_ -> new")
    public <R> OptionSwitch.@NotNull NoneStage<R> some(Function<T, @NotNull R> mapper) {
      return new NoneStage<>(option.map(mapper));
    }

  }

  @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  final class NoneStage<R> implements OptionSwitch {

    Option<@NotNull R> result;

    public @NotNull R none(Supplier<@NotNull R> supplier) {
      return result.getOrCompute(supplier);
    }

  }

}
