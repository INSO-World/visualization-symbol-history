package com.mategka.dava.analyzer.extension;

import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.function.Predicate;

@UtilityClass
public class CollectionsX {

  public <T> boolean onlyElementMatches(Collection<T> collection, Predicate<T> predicate) {
    return collection.size() == 1 && predicate.test(collection.iterator().next());
  }

}
