package com.mategka.dava.analyzer.collections;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class Stack<T> implements Collection<T> {

  @NonNull
  Deque<T> deque;

  public Stack() {
    this(new ArrayDeque<>());
  }

  public Stack(@NotNull Supplier<Deque<T>> dequeSupplier) {
    this(dequeSupplier.get());
  }

  public void push(T e) {
    deque.addFirst(e);
  }

  public T pop() {
    return deque.removeFirst();
  }

  public T peek() {
    return deque.getFirst();
  }

  @Override
  public int size() {
    return deque.size();
  }

  @Override
  public boolean isEmpty() {
    return deque.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return deque.contains(o);
  }

  @Override
  public @NotNull Iterator<T> iterator() {
    return deque.descendingIterator();
  }

  @Override
  public @NotNull Object @NotNull [] toArray() {
    var result = deque.toArray();
    ArrayUtils.reverse(result);
    return result;
  }

  @Override
  public @NotNull <T1> T1 @NotNull [] toArray(@NotNull T1 @NotNull [] a) {
    var result = deque.toArray(a);
    ArrayUtils.reverse(result);
    return result;
  }

  @Override
  public boolean add(T t) {
    return deque.offerFirst(t);
  }

  @Override
  public boolean remove(Object o) {
    return deque.remove(o);
  }

  @Override
  public boolean containsAll(@NotNull Collection<?> c) {
    return deque.containsAll(c);
  }

  @Override
  public boolean addAll(@NotNull Collection<? extends T> c) {
    return c.stream().anyMatch(this::add);
  }

  @Override
  public boolean removeAll(@NotNull Collection<?> c) {
    return deque.removeAll(c);
  }

  @Override
  public boolean retainAll(@NotNull Collection<?> c) {
    return deque.retainAll(c);
  }

  @Override
  public void clear() {
    deque.clear();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Stack<?> stack)) return false;
    return deque.equals(stack.deque);
  }

  @Override
  public int hashCode() {
    return deque.hashCode();
  }

}
