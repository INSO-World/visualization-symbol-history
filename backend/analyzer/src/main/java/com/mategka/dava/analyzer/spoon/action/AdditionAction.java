package com.mategka.dava.analyzer.spoon.action;

import com.mategka.dava.analyzer.spoon.Spoon;
import com.mategka.dava.analyzer.struct.symbol.Subject;

import lombok.NonNull;
import lombok.Value;

@Value(staticConstructor = "of")
public class AdditionAction implements SimpleEditAction {

  Subject oldSubject = null;

  @NonNull
  Subject newSubject;

  @Override
  public String toString() {
    return "+ " + Spoon.descriptorOf(newSubject.getElement());
  }

}
