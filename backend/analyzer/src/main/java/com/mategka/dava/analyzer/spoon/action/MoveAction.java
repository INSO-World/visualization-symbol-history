package com.mategka.dava.analyzer.spoon.action;

import com.mategka.dava.analyzer.struct.symbol.Subject;

import lombok.NonNull;
import lombok.Value;

@Value(staticConstructor = "of")
public class MoveAction implements EditAction {

  @NonNull
  Subject oldSubject;

  @NonNull
  Subject newSubject;

  @Override
  public String toString() {
    return "~ %s -> %s".formatted(oldSubject.toDescriptor(), newSubject.toDescriptor());
  }

}
