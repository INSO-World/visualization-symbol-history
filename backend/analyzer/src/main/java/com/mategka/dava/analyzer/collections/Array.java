package com.mategka.dava.analyzer.collections;

import com.mategka.dava.analyzer.extension.Pair;
import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.extension.traitlike.Streamable;

import lombok.AccessLevel;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Represents an array data structure with better support for type parameters and generic elements.
 * This kind of "typed array" is - similarly to an {@link ArrayList} - backed by an actual array but like an array,
 * will not change its capacity after initialization, making it a memory-efficient type of list when used immutably.
 *
 * @param <T> the element type
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class Array<T> implements List<T>, Streamable<T, AnStream<T>> {

  public final int length;

  T[] array;

  @Delegate(excludes = Streamable.class)
  List<T> list;

  public Array(Object[] array) {
    //noinspection unchecked
    var typedArray = (T[]) array;
    length = typedArray.length;
    this.array = typedArray;
    list = Arrays.asList(typedArray);
  }

  public Array(int length) {
    this(new Object[length]);
  }

  public static <T> Array<T> fromFunction(int length, IntFunction<? extends T> factory) {
    var result = new Array<T>(length);
    for (int i = 0; i < length; i++) {
      result.array[i] = factory.apply(i);
    }
    return result;
  }

  public static <T> Array<T> fromSupplier(int length, Supplier<? extends T> factory) {
    var result = new Array<T>(length);
    for (int i = 0; i < length; i++) {
      result.array[i] = factory.get();
    }
    return result;
  }

  @Contract(value = "_ -> new", pure = true)
  @SafeVarargs
  public static <T> @NotNull Array<T> of(T... elements) {
    return new Array<>(elements);
  }

  public T computeIfNull(int key, Function<@NotNull Integer, T> mappingFunction) {
    Objects.requireNonNull(mappingFunction);
    T t;
    if ((t = get(key)) == null) {
      T newValue;
      if ((newValue = mappingFunction.apply(key)) != null) {
        set(key, newValue);
        return newValue;
      }
    }
    return t;
  }

  public T[] asTypedArray() {
    return array;
  }

  @Override
  public @NotNull AnStream<T> stream() {
    return AnStream.from(array);
  }

  @Override
  public @NotNull AnStream<T> parallelStream() {
    return Streamable.super.parallelStream();
  }

  public Iterable<Pair<Integer, T>> withIndex() {
    return new Iterable<>() {
      @Override
      public @NotNull Iterator<Pair<Integer, T>> iterator() {
        return IntStream.range(0, array.length).boxed().map(Pair.fromLeft(i -> array[i])).iterator();
      }
    };
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof List<?> otherList)) return false;
    return Objects.equals(list, otherList);
  }

  @Override
  public int hashCode() {
    return list.hashCode();
  }

}
