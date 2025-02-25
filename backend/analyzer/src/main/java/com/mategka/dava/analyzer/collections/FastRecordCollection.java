package com.mategka.dava.analyzer.collections;

import com.mategka.dava.analyzer.extension.CollectorsX;
import com.mategka.dava.analyzer.extension.RecordClass;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class FastRecordCollection<R extends Record> implements Collection<R> {

  private final BiMap<Long, R> recordMap = HashBiMap.create();
  private final Map<Object, Long> componentMap = new HashMap<>();
  private final RecordClass<R> recordClass;
  private long counter = 0;

  public FastRecordCollection(Class<R> recordClass) {
    this.recordClass = RecordClass.fromClass(recordClass);
  }

  @Override
  public synchronized boolean add(R r) {
    if (recordMap.inverse().containsKey(r)) {
      return false;
    }
    var components = recordClass.destructure(r);
    if (components.stream().anyMatch(componentMap::containsKey)) {
      throw new IllegalArgumentException("Input object contained duplicate field value");
    }
    internalAdd(r, components);
    return true;
  }

  @Override
  public synchronized boolean addAll(@NotNull Collection<? extends R> c) {
    var relevantElements = c.stream().filter(e -> !recordMap.inverse().containsKey(e)).toList();
    if (relevantElements.isEmpty()) {
      return false;
    }
    Set<Object> componentValues = new HashSet<>(componentMap.keySet());
    Map<R, Collection<Object>> collectionComponents = relevantElements.stream()
      .collect(CollectorsX.mapToValue(recordClass::destructure));
    var allValues = collectionComponents.values().stream().flatMap(Collection::stream).toList();
    for (var value : allValues) {
      if (!componentValues.add(value)) {
        throw new IllegalArgumentException("Input objects contained duplicate field values");
      }
    }
    collectionComponents.forEach(this::internalAdd);
    return true;
  }

  @Override
  public synchronized void clear() {
    recordMap.clear();
    componentMap.clear();
  }

  public synchronized boolean computeIf(Predicate<R> predicate, UnaryOperator<R> supplier) {
    var recordEntry = recordMap.entrySet().stream()
      .filter(e -> predicate.test(e.getValue()))
      .findFirst()
      .orElse(null);
    if (recordEntry == null) {
      return false;
    }
    var recordId = recordEntry.getKey();
    var record = recordEntry.getValue();
    var r = supplier.apply(record);
    if (recordMap.inverse().containsKey(r)) {
      return false;
    }
    var components = recordClass.destructure(r);
    var allMappedComponentsMapToRecord = components.stream()
      .map(componentMap::get)
      .filter(Objects::nonNull)
      .allMatch(recordId::equals);
    if (!allMappedComponentsMapToRecord) {
      throw new IllegalArgumentException("Input object contained duplicate field value");
    }
    internalRemove(record);
    internalAdd(r, components);
    return true;
  }

  @SuppressWarnings("UnusedReturnValue")
  public synchronized <C> boolean computeWhere(Function<R, C> accessor, C c, UnaryOperator<R> supplier) {
    return computeIf(v -> Objects.equals(accessor.apply(v), c), supplier);
  }

  @Override
  public boolean contains(Object o) {
    //noinspection SuspiciousMethodCalls
    return recordMap.inverse().containsKey(o);
  }

  @Override
  public boolean containsAll(@NotNull Collection<?> c) {
    //noinspection SuspiciousMethodCalls
    return c.stream().allMatch(recordMap.inverse()::containsKey);
  }

  public <C> boolean containsWhere(Function<R, C> _accessor, C c) {
    return getWhere(_accessor, c) != null;
  }

  public <K, V> Map<K, V> getMap(Function<R, K> keyAccessor, Function<R, V> valueAccessor) {
    return new CollectionMapView<>(recordMap.values(), keyAccessor, valueAccessor);
  }

  @SuppressWarnings("unused")
  public synchronized <C> R getWhere(Function<R, C> _accessor, C value) {
    return ChainMap.getOnce(componentMap, recordMap, value);
  }

  @Override
  public boolean isEmpty() {
    return recordMap.isEmpty();
  }

  @Override
  public @NotNull Iterator<R> iterator() {
    return recordMap.values().iterator();
  }

  @Override
  public synchronized boolean remove(Object o) {
    if (!recordClass.isInstance(o)) {
      throw new IllegalArgumentException(
        "Only record objects of type %s can be removed from this collection".formatted(recordClass.getSimpleName())
      );
    }
    return internalRemove(o);
  }

  @Override
  public synchronized boolean removeAll(@NotNull Collection<?> c) {
    if (!c.stream().allMatch(recordClass::isInstance)) {
      throw new IllegalArgumentException(
        "Only record objects of type %s can be removed from this collection".formatted(recordClass.getSimpleName())
      );
    }
    return c.stream().anyMatch(this::internalRemove);
  }

  @SuppressWarnings({ "unused", "UnusedReturnValue" })
  public synchronized <C> boolean removeWhere(Function<R, C> _accessor, C c) {
    var id = componentMap.get(c);
    if (id == null) {
      return false;
    }
    var recordInstance = recordMap.remove(id);
    assert recordInstance != null;
    recordClass.destructure(recordInstance).forEach(componentMap::remove);
    return true;
  }

  public synchronized boolean replaceIf(Predicate<R> predicate, R r) {
    return computeIf(predicate, _r -> r);
  }

  public synchronized <C> boolean replaceWhere(Function<R, C> accessor, C c, R r) {
    return replaceIf(v -> Objects.equals(accessor.apply(v), c), r);
  }

  @Override
  public synchronized boolean retainAll(@NotNull Collection<?> c) {
    if (!c.stream().allMatch(recordClass::isInstance)) {
      throw new IllegalArgumentException(
        "Only record objects of type %s can be retained by this collection".formatted(recordClass.getSimpleName())
      );
    }
    var recordsToRemove = new HashSet<>(recordMap.values());
    //noinspection SuspiciousMethodCalls
    recordsToRemove.removeAll(c);
    if (recordsToRemove.isEmpty()) {
      return false;
    }
    recordsToRemove.forEach(this::internalRemove);
    return true;
  }

  @Override
  public int size() {
    return recordMap.size();
  }

  @Override
  public @NotNull Object @NotNull [] toArray() {
    return recordMap.values().toArray();
  }

  @Override
  public @NotNull <T1> T1 @NotNull [] toArray(@NotNull T1 @NotNull [] a) {
    return recordMap.values().toArray(a);
  }

  private synchronized void internalAdd(R r, Iterable<Object> components) {
    var id = counter++;
    components.forEach(c -> componentMap.put(c, id));
    recordMap.put(id, r);
  }

  private synchronized boolean internalRemove(Object o) {
    //noinspection unchecked
    var record = (R) o;
    var components = recordClass.destructure(record);
    var removedRecordId = recordMap.inverse().remove(record);
    if (removedRecordId == null) {
      return false;
    }
    components.forEach(componentMap::remove);
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof FastRecordCollection<?> that)) return false;
    if (recordMap.size() != that.recordMap.size()) return false;
    return Objects.equals(recordClass, that.recordClass) && componentMap.keySet().stream()
      .allMatch(c -> Objects.equals(getWhere(null, c), that.getWhere(null, c)));
  }

  @Override
  public int hashCode() {
    return Objects.hash(recordClass, recordMap.values(), componentMap.keySet());
  }

}
