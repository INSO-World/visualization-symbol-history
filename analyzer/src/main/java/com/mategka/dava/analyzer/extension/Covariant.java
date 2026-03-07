package com.mategka.dava.analyzer.extension;

import com.mategka.dava.analyzer.extension.struct.Pair;

import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
@UtilityClass
public class Covariant {

  public <T> Collection<T> collection(Collection<? extends T> collection) {
    return (Collection<T>) collection;
  }

  public <T> List<T> list(List<? extends T> list) {
    return (List<T>) list;
  }

  public <L, R> Pair<L, R> pair(Pair<? extends L, ? extends R> pair) {
    return (Pair<L, R>) pair;
  }

  public <T> Set<T> set(Set<? extends T> set) {
    return (Set<T>) set;
  }

  public <T> Stream<T> stream(Stream<? extends T> stream) {
    return (Stream<T>) stream;
  }

}
