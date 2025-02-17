package com.mategka.dava.analyzer.collections;

import com.mategka.dava.analyzer.extension.CollectorsX;
import com.mategka.dava.analyzer.extension.RecordsX;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class FastRecordCollection<T extends Record> implements Collection<T> {

  private final Class<T> recordClass;
  private final BiMap<Long, T> recordMap = HashBiMap.create();
  private final Map<Object, Long> componentMap = new HashMap<>();
  private final Function<T, Stream<Object>> componentExtractor;
  private long counter = 0;

  public FastRecordCollection(Class<T> recordClass) {
    this.recordClass = recordClass;
    componentExtractor = RecordsX.componentExtractor(recordClass);
  }

  public <K, V> Map<K, V> getMap(Function<T, K> keyAccessor, Function<T, V> valueAccessor) {
    return new CollectionMapView<>(recordMap.values(), keyAccessor, valueAccessor);
  }

  @SuppressWarnings("unused")
  public synchronized <C> T getWhere(Function<T, C> _accessor, C value) {
    return ChainMap.getOnce(componentMap, recordMap, value);
  }

  @Override
  public int size() {
    return recordMap.size();
  }

  @Override
  public boolean isEmpty() {
    return recordMap.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    //noinspection SuspiciousMethodCalls
    return recordMap.inverse().containsKey(o);
  }

  public <C> boolean containsWhere(Function<T, C> _accessor, C c) {
    return getWhere(_accessor, c) != null;
  }

  @Override
  public @NotNull Iterator<T> iterator() {
    return recordMap.values().iterator();
  }

  @Override
  public @NotNull Object @NotNull [] toArray() {
    return recordMap.values().toArray();
  }

  @Override
  public @NotNull <T1> T1 @NotNull [] toArray(@NotNull T1 @NotNull [] a) {
    return recordMap.values().toArray(a);
  }

  @Override
  public synchronized boolean add(T t) {
    if (recordMap.inverse().containsKey(t)) {
      return false;
    }
    var components = componentExtractor.apply(t).toList();
    if (components.stream().anyMatch(componentMap::containsKey)) {
      throw new IllegalArgumentException("Input object contained duplicate field value");
    }
    internalAdd(t, components);
    return true;
  }

  private synchronized void internalAdd(T t, Iterable<Object> components) {
    var id = counter++;
    components.forEach(c -> componentMap.put(c, id));
    recordMap.put(id, t);
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

  @SuppressWarnings({ "unused", "UnusedReturnValue" })
  public synchronized <C> boolean removeWhere(Function<T, C> _accessor, C c) {
    var id = componentMap.get(c);
    if (id == null) {
      return false;
    }
    var recordInstance = recordMap.remove(id);
    assert recordInstance != null;
    componentExtractor.apply(recordInstance).forEach(componentMap::remove);
    return true;
  }

  public synchronized <C> boolean replaceWhere(Function<T, C> accessor, C c, T t) {
    return replaceIf(v -> Objects.equals(accessor.apply(v), c), t);
  }

  public synchronized boolean replaceIf(Predicate<T> predicate, T t) {
    return computeIf(predicate, _t -> t);
  }

  @SuppressWarnings("UnusedReturnValue")
  public synchronized <C> boolean computeWhere(Function<T, C> accessor, C c, UnaryOperator<T> supplier) {
    return computeIf(v -> Objects.equals(accessor.apply(v), c), supplier);
  }

  public synchronized boolean computeIf(Predicate<T> predicate, UnaryOperator<T> supplier) {
    var recordEntry = recordMap.entrySet().stream()
      .filter(e -> predicate.test(e.getValue()))
      .findFirst()
      .orElse(null);
    if (recordEntry == null) {
      return false;
    }
    var recordId = recordEntry.getKey();
    var record = recordEntry.getValue();
    var t = supplier.apply(record);
    if (recordMap.inverse().containsKey(t)) {
      return false;
    }
    var components = componentExtractor.apply(t).toList();
    var allMappedComponentsMapToRecord = components.stream()
      .map(componentMap::get)
      .filter(Objects::nonNull)
      .allMatch(recordId::equals);
    if (!allMappedComponentsMapToRecord) {
      throw new IllegalArgumentException("Input object contained duplicate field value");
    }
    internalRemove(record);
    internalAdd(t, components);
    return true;
  }

  private synchronized boolean internalRemove(Object o) {
    //noinspection unchecked
    var record = (T) o;
    var components = componentExtractor.apply(record);
    var removedRecordId = recordMap.inverse().remove(record);
    if (removedRecordId == null) {
      return false;
    }
    components.forEach(componentMap::remove);
    return true;
  }

  @Override
  public boolean containsAll(@NotNull Collection<?> c) {
    //noinspection SuspiciousMethodCalls
    return c.stream().allMatch(recordMap.inverse()::containsKey);
  }

  @Override
  public synchronized boolean addAll(@NotNull Collection<? extends T> c) {
    var relevantElements = c.stream().filter(e -> !recordMap.inverse().containsKey(e)).toList();
    if (relevantElements.isEmpty()) {
      return false;
    }
    Set<Object> componentValues = new HashSet<>(componentMap.keySet());
    Map<T, Collection<Object>> collectionComponents = relevantElements.stream()
      .collect(CollectorsX.mapToValue(r -> componentExtractor.apply(r).toList()));
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
  public synchronized boolean removeAll(@NotNull Collection<?> c) {
    if (!c.stream().allMatch(recordClass::isInstance)) {
      throw new IllegalArgumentException(
        "Only record objects of type %s can be removed from this collection".formatted(recordClass.getSimpleName())
      );
    }
    return c.stream().anyMatch(this::internalRemove);
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
  public synchronized void clear() {
    recordMap.clear();
    componentMap.clear();
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
