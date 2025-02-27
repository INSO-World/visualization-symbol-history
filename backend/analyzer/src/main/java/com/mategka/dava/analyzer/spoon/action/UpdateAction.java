package com.mategka.dava.analyzer.spoon.action;

import com.mategka.dava.analyzer.struct.symbol.Subject;

import lombok.NonNull;
import lombok.Value;

@Value(staticConstructor = "of")
public class UpdateAction implements EditAction {

  @NonNull
  Subject oldSubject;

  @NonNull
  Subject newSubject;

  @Override
  public String toString() {
    var oldDescriptor = oldSubject.toDescriptor();
    var newDescriptor = newSubject.toDescriptor();
    if (oldDescriptor.equals(newDescriptor)) {
      return "* " + oldDescriptor;
    }
    return "* %s -> %s".formatted(oldSubject.toDescriptor(), newSubject.toDescriptor());
  }

}
