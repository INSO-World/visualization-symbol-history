package com.mategka.dava.analyzer.spoon.action;

import com.mategka.dava.analyzer.struct.symbol.Subject;

import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtElement;

@Value(staticConstructor = "of")
public class DeletionAction implements SimpleEditAction {

  @NonNull
  Subject oldSubject;

  @Override
  public Subject getReferenceSubject() {
    return oldSubject;
  }

  public CtElement getOldElement() {
    return oldSubject.getElement();
  }

  public CtElement getOldParent() {
    return oldSubject.getParent();
  }

  @Override
  public String toString() {
    return "- " + oldSubject.toDescriptor();
  }

}
