package com.mategka.dava.analyzer.extension;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
@UtilityClass
public class Covariant {

  public <T> List<T> list(List<? extends T> list) {
    return (List<T>) list;
  }

  public <T> Set<T> set(Set<? extends T> set) {
    return (Set<T>) set;
  }

  public <T> Stream<T> stream(Stream<? extends T> stream) {
    return (Stream<T>) stream;
  }

  public <L, R> Pair<L, R> pair(Pair<? extends L, ? extends R> pair) {
    return (Pair<L, R>) pair;
  }

}
