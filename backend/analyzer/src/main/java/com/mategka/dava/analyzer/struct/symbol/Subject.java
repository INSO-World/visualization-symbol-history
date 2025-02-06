package com.mategka.dava.analyzer.struct.symbol;

import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtElement;

@Value(staticConstructor = "of")
public class Subject {

  @NonNull
  CtElement element;

  @NonNull
  CtElement parent;

}
