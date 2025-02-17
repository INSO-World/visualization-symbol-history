package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.spoon.Spoon;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spoon.reflect.declaration.CtElement;

import java.util.Objects;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(staticName = "of")
@ToString
public class Subject {

  @NonNull
  CtElement element;

  @NonNull
  CtElement parent;

  public String toDescriptor() {
    return Spoon.descriptorOf(element);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Subject subject)) return false;
    return Objects.equals(element, subject.element) && Objects.equals(parent, subject.parent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(element, parent);
  }

}
