package com.mategka.dava.analyzer.collections;

import com.mategka.dava.analyzer.extension.Pair;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.*;

/**
 * A type of map capable of mapping two different kinds of keys to a value such
 * that any one key is sufficient to retrieve the mapped value in near-constant
 * time.
 * <p>
 * For best performance, make sure to mitigate the risk of hash collisions for
 * the objects used as keys. This means they should use different
 * {@link Object#hashCode()} implementations if the semantics for both are
 * value-based (i.e., if keys are equal when their fields are).
 * @param <K1> the first key type
 * @param <K2> the second key type
 * @param <V> the value type
 */
public class DualKeyHashMap<K1, K2, V> {

  private final Map<Object, V> map = new HashMap<>();
  private final Map<V, Pair<K1, K2>> valueToKeys = new HashMap<>();

  @CanIgnoreReturnValue
  public boolean put(K1 key1, K2 key2, V value) {
    boolean mappingChanged = false;
    V oldValueForKey1 = getByKey1(key1);
    if (oldValueForKey1 != null && oldValueForKey1 != value) {
      removeByValue(oldValueForKey1);
      mappingChanged = true;
    }
    V oldValueForKey2 = getByKey2(key2);
    if (oldValueForKey2 != null && oldValueForKey2 != value) {
      removeByValue(oldValueForKey2);
      mappingChanged = true;
    }

    if (oldValueForKey1 == null && oldValueForKey2 == null) {
      mappingChanged = true;
    }

    map.put(key1, value);
    map.put(key2, value);
    valueToKeys.put(value, Pair.of(key1, key2));

    return mappingChanged;
  }

  public V getByKey1(K1 key1) {
    return map.get(key1);
  }

  public V getByKey2(K2 key2) {
    return map.get(key2);
  }

  @CanIgnoreReturnValue
  public V removeByKey1(K1 key1) {
    V value = map.get(key1);
    if (value != null) {
      removeByValue(value);
    }
    return value;
  }

  @CanIgnoreReturnValue
  public V removeByKey2(K2 key2) {
    V value = map.get(key2);
    if (value != null) {
      removeByValue(value);
    }
    return value;
  }

  @CanIgnoreReturnValue
  public boolean removeByValue(V value) {
    Pair<K1, K2> keys = valueToKeys.remove(value);
    if (keys == null) {
      return false;
    }
    map.remove(keys.left());
    map.remove(keys.right());
    return true;
  }

  public boolean containsKey1(K1 key1) {
    return map.containsKey(key1);
  }

  public boolean containsKey2(K2 key2) {
    return map.containsKey(key2);
  }

  public int size() {
    return valueToKeys.size();
  }

  public boolean isEmpty() {
    return valueToKeys.isEmpty();
  }

  public void clear() {
    map.clear();
    valueToKeys.clear();
  }

  public Set<V> values() {
    return new HashSet<>(valueToKeys.keySet());
  }

}
