package com.mategka.dava.analyzer.extension;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

@Accessors(fluent = true)
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(staticName = "of")
public class Id<T> {

  T value;

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof Id<?> id)) return false;
    return value == id.value;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(value);
  }

  @Override
  public String toString() {
    return value.toString();
  }

}
