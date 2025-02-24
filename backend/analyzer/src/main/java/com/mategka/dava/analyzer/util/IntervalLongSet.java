package com.mategka.dava.analyzer.util;

import com.mategka.dava.analyzer.extension.option.Option;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

public class IntervalLongSet implements Set<Long> {

  private final NavigableSet<Entry> internalSet = new TreeSet<>(Comparator.comparingLong(Entry::start));

  @Override
  public boolean add(Long aLong) {
    Objects.requireNonNull(aLong);
    var unitEntry = new Entry(aLong, 1);
    if (isEmpty()) {
      internalSet.add(unitEntry);
      return true;
    }
    // assert floor != null || ceiling != null
    var floor = internalSet.floor(unitEntry);
    if (floor != null) {
      if (floor.containsBeforeEnding(aLong)) {
        return false;
      }
      if (aLong > floor.end + 1) {
        internalSet.add(unitEntry);
        return true;
      }
      assert aLong == floor.end + 1;
      internalSet.remove(floor);
      var ceiling = internalSet.higher(floor);
      if (ceiling == null || ceiling.start > floor.end + 2) {
        internalSet.add(new Entry(floor.start, aLong));
        return true;
      }
      internalSet.remove(ceiling);
      internalSet.add(new Entry(floor.start, ceiling.end));
      return true;
    }
    var ceiling = internalSet.ceiling(unitEntry);
    assert ceiling != null;
    if (ceiling.start > aLong + 1) {
      internalSet.add(unitEntry);
      return true;
    }
    assert ceiling.start == aLong + 1;
    internalSet.remove(ceiling);
    internalSet.add(new Entry(aLong, ceiling.end));
    return true;
  }

  @Override
  public boolean addAll(Collection<? extends Long> c) {
    return false;
  }

  @Override
  public void clear() {
    internalSet.clear();
  }

  @Override
  public boolean contains(Object o) {
    if (!(o instanceof Number n)) {
      return false;
    }
    final long value = n.longValue();
    return internalFloor(value)
      .map(e -> e.containsBeforeEnding(value))
      .getOrElse(false);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return false;
  }

  @Override
  public boolean isEmpty() {
    return internalSet.isEmpty();
  }

  @Override
  public @NotNull Iterator<Long> iterator() {
    return internalSet.stream()
      .flatMapToLong(e -> LongStream.rangeClosed(e.start, e.end))
      .iterator();
  }

  public LongStream longStream() {
    return StreamSupport.longStream(spliterator(), false);
  }

  public LongStream parallelLongStream() {
    return StreamSupport.longStream(spliterator(), true);
  }

  @Override
  public boolean remove(Object o) {
    if (isEmpty()) {
      return false;
    }
    if (!(o instanceof Number n)) {
      return false;
    }
    var value = n.longValue();
    var floor = internalSet.floor(new Entry(value, 1));
    if (floor == null || !floor.containsBeforeEnding(value)) {
      return false;
    }
    internalSet.remove(floor);
    var predecessorCount = value - floor.start;
    var successorCount = floor.end - value;
    if (predecessorCount > 0) {
      internalSet.add(new Entry(floor.start, value - 1));
    }
    if (successorCount > 0) {
      internalSet.add(new Entry(value + 1, floor.end));
    }
    return true;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return false;
  }

  @Override
  public boolean removeIf(Predicate<? super Long> filter) {
    return Set.super.removeIf(filter);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return false;
  }

  @Override
  public int size() {
    return (int) Math.min(sizeLong(), Integer.MAX_VALUE);
  }

  public long sizeLong() {
    return internalSet.stream().mapToLong(Entry::length).sum();
  }

  @Override
  public @NotNull Spliterator.OfLong spliterator() {
    return new IntervalLongSetSpliterator(internalSet.spliterator());
  }

  @Override
  public Long @NotNull [] toArray() {
    var result = new Long[size()];
    var iterator = iterator();
    for (int i = 0; iterator.hasNext(); i++) {
      result[i] = iterator.next();
    }
    return result;
  }

  @Override
  public <T> T @NotNull [] toArray(T @NotNull [] a) {
    return null;
  }

  private Option<Entry> internalFloor(Long value) {
    return Option.fromNullable(internalSet.floor(new Entry(value, 1)));
  }

  private record Entry(long start, long end) {

    public boolean contains(long value) {
      return start <= value && value <= end;
    }

    public long length() {
      return end - start + 1;
    }

    private boolean containsBeforeEnding(long value) {
      return value <= end;
    }

  }

  private class IntervalLongSetSpliterator implements Spliterator.OfLong {

    private final Spliterator<Entry> internalSpliterator;
    private long current = -1;
    private long end = -1;

    private IntervalLongSetSpliterator(Spliterator<Entry> internalSpliterator) {
      this.internalSpliterator = internalSpliterator;
    }

    @Override
    public int characteristics() {
      return Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED | Spliterator.NONNULL;
    }

    @Override
    public long estimateSize() {
      return internalSpliterator.estimateSize() * sizeLong() / internalSet.size();
    }

    @Override
    public Comparator<? super Long> getComparator() {
      return OfLong.super.getComparator();
    }

    @Override
    public boolean tryAdvance(LongConsumer action) {
      while (true) {
        if (current <= end) {
          action.accept(current++);
          return true;
        } else if (!internalSpliterator.tryAdvance(e -> {
          current = e.start;
          end = e.end;
        })) {
          return false;
        }
      }
    }

    @Override
    public OfLong trySplit() {
      var split = internalSpliterator.trySplit();
      return (split == null) ? null : new IntervalLongSetSpliterator(split);
    }

  }

}
