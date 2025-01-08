package com.mategka.dava.analyzer.util;

import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@UtilityClass
public class Optionals {



  public <T> Optional<T> cast(Optional<? super T> optional, Class<T> targetType) {
    return optional.filter(targetType::isInstance).map(targetType::cast);
  }

  public <T> Optional<T> when(boolean condition, Supplier<T> supplier) {
    return condition ? Optional.of(supplier.get()) : Optional.empty();
  }

  public <T> Optional<T> getFirst(SequencedCollection<? extends T> collection) {
    return when(!collection.isEmpty(), collection::getFirst);
  }

  public <T> Optional<T> getFirst(Iterable<? extends T> iterable) {
    var iterator = iterable.iterator();
    return when(iterator.hasNext(), iterator::next);
  }

  public <T> Optional<T> getFirst(T[] array) {
    return when(array.length > 0, () -> array[0]);
  }

}
