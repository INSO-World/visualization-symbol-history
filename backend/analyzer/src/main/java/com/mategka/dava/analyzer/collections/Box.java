package com.mategka.dava.analyzer.collections;

import com.mategka.dava.analyzer.extension.ObjectsX;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.*;

@SuppressWarnings("unused")
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode
public class Box<T> implements Iterable<T>, Comparable<Object> {

  T value;

  @Contract(" -> new")
  public static <T> @NotNull Box<T> empty() {
    return new Box<>(null);
  }

  public boolean isNull() {
    return value == null;
  }

  public boolean contains(Object o) {
    return Objects.equals(value, o);
  }

  @Override
  public @NotNull Iterator<T> iterator() {
    return toList().iterator();
  }

  public List<T> toList() {
    return List.of(value);
  }

  @Override
  public void forEach(Consumer<? super T> action) {
    action.accept(value);
  }

  public T set(@NotNull T t) {
    var result = value;
    internalSet(t);
    return result;
  }

  private T internalSet(T value) {
    if (value instanceof Box<?> box) {
      throw new IllegalArgumentException("Boxes cannot contain other boxes");
    }
    return this.value = value;
  }

  public T get() {
    return value;
  }

  public boolean removeIf(Object value) {
    if (contains(value)) {
      clear();
      return true;
    }
    return false;
  }

  public boolean removeIf(@NotNull Predicate<? super T> filter) {
    if (filter.test(value)) {
      clear();
      return true;
    }
    return false;
  }

  public void clear() {
    value = null;
  }

  public Optional<T> toOptional() {
    return Optional.ofNullable(value);
  }

  public T getOrDefault(T defaultValue) {
    return toOptional().orElse(defaultValue);
  }

  public @Nullable T setIfAbsent(T value) {
    if (this.value == null) {
      internalSet(value);
      return null;
    }
    return this.value;
  }

  public boolean replace(T oldValue, T newValue) {
    if (contains(oldValue)) {
      internalSet(newValue);
      return true;
    }
    return false;
  }

  public T computeIfAbsent(@NotNull Supplier<? extends T> supplier) {
    if (value == null) {
      internalSet(supplier.get());
    }
    return value;
  }

  public T computeIfPresent(@NotNull Function<? super T, ? extends T> remappingFunction) {
    if (value == null) {
      return null;
    }
    return internalSet(remappingFunction.apply(value));
  }

  public T compute(@NotNull Function<? super @Nullable T, ? extends T> remappingFunction) {
    return internalSet(remappingFunction.apply(value));
  }

  public T merge(@NotNull T value, @NotNull BiFunction<? super T, ? super T, ? extends T> remappingFunction) {
    if (this.value == null) {
      return internalSet(value);
    }
    return internalSet(remappingFunction.apply(this.value, value));
  }

  @Override
  public int compareTo(@NotNull Object o) {
    var otherValue = (o instanceof Box<?> box) ? box.value : o;
    return ObjectsX.nullsFirstComparator().compare(value, otherValue);
  }

}
