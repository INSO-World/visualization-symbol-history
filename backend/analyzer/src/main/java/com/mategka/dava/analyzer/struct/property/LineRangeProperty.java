package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.extension.Pair;
import com.mategka.dava.analyzer.struct.property.index.PropertyKey;

import spoon.reflect.declaration.CtElement;

@PropertyKey("lines")
public record LineRangeProperty(Pair<Integer, Integer> value) implements SimpleProperty<Pair<Integer, Integer>> {

  public static LineRangeProperty fromElement(CtElement element) {
    return new LineRangeProperty(getLineNumbers(element));
  }

  public static Pair<Integer, Integer> getLineNumbers(CtElement element) {
    var position = element.getPosition();
    if (!position.isValidPosition()) {
      throw new IllegalStateException("Symbol element should have had valid position");
    }
    return Pair.of(position.getLine(), position.getEndLine());
  }

  @Override
  public String toString() {
    return "%d:%d".formatted(value.left(), value.right());
  }

}
