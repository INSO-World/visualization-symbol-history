package com.mategka.dava.analyzer.collections;

import lombok.AccessLevel;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IdentityHashSet<T> implements Set<T> {

  @Delegate
  Set<T> delegate = Collections.newSetFromMap(new IdentityHashMap<>());

}
