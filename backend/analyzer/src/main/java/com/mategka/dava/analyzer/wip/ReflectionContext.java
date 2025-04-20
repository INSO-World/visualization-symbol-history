package com.mategka.dava.analyzer.wip;

import java.lang.reflect.Array;
import java.lang.reflect.InaccessibleObjectException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReflectionContext {

  private static final Set<String> BOXED_TYPES = Stream.of(
    Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class
  ).map(Class::getSimpleName).collect(Collectors.toSet());

  private static final long HEADER_SIZE = 16;
  private static final long REFERENCE_SIZE = 8;
  private static final long ALIGNMENT_SIZE = 8;

  private final Map<Object, Long> objectToSize = new IdentityHashMap<>();

  public long estimateSize(Object object) {
    if (object == null) {
      return 0;
    }
    if (objectToSize.containsKey(object)) {
      return objectToSize.get(object);
    }
    if (object.getClass().isPrimitive() || BOXED_TYPES.contains(object.getClass().getSimpleName())) {
      return ALIGNMENT_SIZE;
    }
    return estimateSize(object, Collections.newSetFromMap(new IdentityHashMap<>()));
  }

  public Set<Object> getReferences(Object object) {
    if (object == null || object.getClass().isPrimitive() || BOXED_TYPES.contains(object.getClass().getSimpleName())) {
      return Collections.emptySet();
    }
    var references = Collections.newSetFromMap(new IdentityHashMap<>());
    getReferences(object, references);
    references.remove(object);
    return references;
  }

  private long estimateArraySize(Object array, Set<Object> visitedReferences) {
    long result = HEADER_SIZE;
    var innerType = array.getClass().getComponentType();
    var length = Array.getLength(array);
    if (innerType.isPrimitive() || BOXED_TYPES.contains(innerType.getSimpleName())) {
      return ALIGNMENT_SIZE * length;
    }
    result += REFERENCE_SIZE * Array.getLength(array);
    for (int i = 0; i < length; i++) {
      result += estimateSize(Array.get(array, i), visitedReferences);
    }
    return result;
  }

  private long estimateSize(Object object, Set<Object> visitedReferences) {
    if (object == null) {
      return 0;
    }
    if (objectToSize.containsKey(object)) {
      return objectToSize.get(object);
    }
    visitedReferences.add(object);
    if (object instanceof String string) {
      var size = string.length() + HEADER_SIZE;
      objectToSize.put(string, size);
      return size;
    }
    var clazz = object.getClass();
    if (clazz.isArray()) {
      return estimateArraySize(object, visitedReferences);
    }
    long result = HEADER_SIZE;
    while (clazz != null) {
      var fields = clazz.getDeclaredFields();
      for (var field : fields) {
        try {
          field.setAccessible(true);
          var value = field.get(object);
          if (value == null) {
            continue;
          }
          if (value.getClass().isPrimitive() || BOXED_TYPES.contains(value.getClass().getSimpleName())) {
            result += ALIGNMENT_SIZE;
          } else {
            if (!visitedReferences.contains(value)) {
              result += estimateSize(value, visitedReferences);
            }
            result += REFERENCE_SIZE;
          }
        } catch (IllegalAccessException | InaccessibleObjectException e) {
          // Ignored
        }
      }
      clazz = clazz.getSuperclass();
    }
    objectToSize.put(object, result);
    return result;
  }

  private void getArrayReferences(Object array, Set<Object> references) {
    var innerType = array.getClass().getComponentType();
    if (innerType.isPrimitive() || BOXED_TYPES.contains(innerType.getSimpleName())) {
      return;
    }
    for (Object component : (Object[]) array) {
      getReferences(component, references);
    }
  }

  private void getReferences(Object object, Set<Object> references) {
    if (object == null) {
      return;
    }
    references.add(object);
    if (object instanceof String) {
      return;
    }
    var clazz = object.getClass();
    if (clazz.isArray()) {
      getArrayReferences(object, references);
      return;
    }
    while (clazz != null) {
      var fields = clazz.getDeclaredFields();
      for (var field : fields) {
        field.setAccessible(true);
        try {
          var value = field.get(object);
          if (value == null || value.getClass().isPrimitive() || BOXED_TYPES.contains(
            value.getClass().getSimpleName())) {
            continue;
          }
          if (!references.contains(value)) {
            getReferences(value, references);
          }
        } catch (IllegalAccessException | InaccessibleObjectException e) {
          // Ignored
        }
      }
      clazz = clazz.getSuperclass();
    }
  }

}
