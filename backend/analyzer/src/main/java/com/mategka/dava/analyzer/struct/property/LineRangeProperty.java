package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import org.apache.commons.lang3.tuple.Pair;

@PropertyKey("lines")
public record LineRangeProperty(Pair<Integer, Integer> value) implements SimpleProperty<Pair<Integer, Integer>> {

  @Override
  public String toString() {
    return "%d:%d".formatted(value.getLeft(), value.getRight());
  }

}
