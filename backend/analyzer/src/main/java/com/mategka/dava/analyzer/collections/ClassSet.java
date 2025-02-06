package com.mategka.dava.analyzer.collections;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class ClassSet implements Set<Class<?>> {

  @Delegate
  Set<Class<?>> set;

  public ClassSet() {
    this(Collections.newSetFromMap(new IdentityHashMap<>()));
  }

  public ClassSet(@NotNull Supplier<Set<Class<?>>> setSupplier) {
    this(setSupplier.get());
  }

  public static @NotNull ClassSet of(Class<?>... classes) {
    var result = new ClassSet();
    result.addAll(Arrays.asList(classes));
    return result;
  }

  @Override
  public boolean add(Class<?> clazz) {
    return !containsClass(clazz) && set.add(clazz);
  }

  @Override
  public boolean remove(Object o) {
    if (!(o instanceof Class<?> clazz)) {
      return false;
    }
    var subtypes = set.stream().filter(clazz::isAssignableFrom).toList();
    //noinspection SlowAbstractSetRemoveAll
    return set.removeAll(subtypes);
  }

  public boolean containsClassOf(@NotNull Object object) {
    return containsClass(object.getClass());
  }

  public boolean containsClass(@NotNull Class<?> clazz) {
    return set.stream().anyMatch(c -> c.isAssignableFrom(clazz));
  }

  @Override
  public boolean contains(@NotNull Object object) {
    if (object instanceof Class<?> c) {
      return containsClass(c);
    }
    return false;
  }

}
