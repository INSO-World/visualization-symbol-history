package com.mategka.dava.analyzer.collections.relationship;

import lombok.Value;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Accessors(fluent = true)
@Value(staticConstructor = "create")
public class Mapping<S, T, M> {

  S source;
  T target;
  M metadata;

  @Contract("_, _ -> new")
  public static <S, T, M> @NotNull Mapping<@Nullable S, @NotNull T, @Nullable M> createAddition(@NotNull T target,
                                                                                                @Nullable M metadata) {
    return new Mapping<>(null, target, metadata);
  }

  @Contract("_, _ -> new")
  public static <S, T, M> @NotNull Mapping<@NotNull S, @Nullable T, @Nullable M> createDeletion(@NotNull S source,
                                                                                                @Nullable M metadata) {
    return new Mapping<>(source, null, metadata);
  }

  @Contract("_, _ -> new")
  public static <S, T, M> @NotNull Mapping<@NotNull S, @NotNull T, @Nullable M> createUnannotated(@NotNull S source,
                                                                                                  @NotNull T target) {
    return new Mapping<>(source, target, null);
  }

  public boolean isAddition() {
    return source == null;
  }

  public boolean isDeletion() {
    return target == null;
  }

  public boolean isUnannotated() {
    return metadata == null;
  }

}
