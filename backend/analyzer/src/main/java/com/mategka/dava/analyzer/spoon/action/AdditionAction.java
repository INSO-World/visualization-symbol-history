package com.mategka.dava.analyzer.spoon.action;

import com.mategka.dava.analyzer.struct.symbol.Subject;

import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Delegate;
import spoon.reflect.declaration.CtElement;

@Value(staticConstructor = "of")
public class AdditionAction implements SimpleEditAction {

  @NonNull
  Subject newSubject;

  @Override
  public Subject getReferenceSubject() {
    return newSubject;
  }

  public CtElement getNewElement() {
    return newSubject.getElement();
  }

  public CtElement getNewParent() {
    return newSubject.getParent();
  }

  @Override
  public String toString() {
    return "+ " + newSubject.toDescriptor();
  }

}
