package com.mategka.dava.analyzer.spoon.action;

import com.mategka.dava.analyzer.struct.symbol.Subject;

import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtElement;

@Value(staticConstructor = "of")
public class AdditionAction implements SimpleEditAction {

  @NonNull
  Subject newSubject;

  public CtElement getNewElement() {
    return newSubject.getElement();
  }

  public CtElement getNewParent() {
    return newSubject.getParent();
  }

  @Override
  public Subject getReferenceSubject() {
    return newSubject;
  }

  @Override
  public String toString() {
    return "+ " + newSubject.toDescriptor();
  }

}
