package com.mategka.dava.analyzer.spoon.action;

import com.mategka.dava.analyzer.struct.symbol.Subject;

import spoon.reflect.declaration.CtElement;

public sealed interface SimpleEditAction extends EditAction permits AdditionAction, DeletionAction {

  default CtElement getReferenceElement() {
    return getReferenceSubject().getElement();
  }

  default CtElement getReferenceParent() {
    return getReferenceSubject().getParent();
  }

  Subject getReferenceSubject();

}
