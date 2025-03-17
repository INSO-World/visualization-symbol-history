package com.mategka.dava.analyzer.collections;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SlidingWindow<E> implements Collection<E> {

  E[] elements;
  final int capacity;
  int headPointer;
  int size;

  public SlidingWindow(int windowSize) {
    capacity = windowSize;
    clear();
  }

  @Override
  public int size() {
    return size;
  }

  public int capacity() {
    return capacity;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public boolean contains(Object o) {
    return Arrays.asList(elements).contains(o);
  }

  @Override
  public @NotNull Iterator<E> iterator() {
    return Arrays.asList(toTypedArray()).iterator();
  }

  @Override
  public @NotNull Object @NotNull [] toArray() {
    return toTypedArray();
  }

  public @NotNull E @NotNull [] toTypedArray() {
    //noinspection unchecked
    E[] result = (E[]) new Object[size];
    System.arraycopy(elements, headPointer, result, 0, size);
    System.arraycopy(elements, 0, result, size, headPointer);
    return result;
  }

  @Override
  public @NotNull <T> T @NotNull [] toArray(T @NotNull [] a) {
    //noinspection unchecked
    T[] base = (T[]) toArray();
    if (a.length < size) {
      return base;
    }
    System.arraycopy(base, 0, a, 0, base.length);
    if (a.length > size) {
      a[size] = null;
    }
    return a;
  }

  @Override
  public synchronized boolean add(E e) {
    elements[headPointer] = e;
    headPointer = (headPointer + 1) % capacity;
    if (size < capacity) {
      size++;
    }
    return true;
  }

  public synchronized E accept(E e) {
    if (size < capacity) {
      add(e);
      return null;
    }
    E priorElement = elements[headPointer];
    add(e);
    return priorElement;
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException("Individual values cannot be removed from a SlidingWindow");
  }

  @Override
  public boolean containsAll(@NotNull Collection<?> c) {
    return new HashSet<>(Arrays.asList(elements).subList(0, size)).containsAll(c);
  }

  @Override
  public synchronized boolean addAll(@NotNull Collection<? extends E> c) {
    if (c.isEmpty()) {
      return false;
    }
    if (c.size() < capacity) {
      c.forEach(this::add);
    } else {
      //noinspection unchecked
      E[] lastElements = (E[]) c.stream().skip(c.size() - capacity).toArray(Object[]::new);
      System.arraycopy(lastElements, 0, elements, headPointer, capacity - headPointer);
      System.arraycopy(lastElements, capacity - headPointer, elements, 0, headPointer);
      // headPointer remains unchanged
      size = capacity;
    }
    return true;
  }

  @Override
  public boolean removeAll(@NotNull Collection<?> c) {
    throw new UnsupportedOperationException("Individual values cannot be removed from a SlidingWindow");
  }

  @Override
  public boolean removeIf(@NotNull Predicate<? super E> filter) {
    throw new UnsupportedOperationException("Individual values cannot be removed from a SlidingWindow");
  }

  @Override
  public boolean retainAll(@NotNull Collection<?> c) {
    throw new UnsupportedOperationException("Individual values cannot be removed from a SlidingWindow");
  }

  @Override
  public synchronized void clear() {
    //noinspection unchecked
    elements = (E[]) new Object[capacity];
    headPointer = 0;
    size = 0;
  }

  @Override
  public @NotNull Stream<E> stream() {
    return Arrays.stream(toTypedArray());
  }

  @Override
  public @NotNull Stream<E> parallelStream() {
    return stream().parallel();
  }

  @Override
  public @NotNull Spliterator<E> spliterator() {
    return stream().spliterator();
  }

}
