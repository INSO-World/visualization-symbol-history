package com.mategka.dava.analyzer.spoon.action;

import com.mategka.dava.analyzer.struct.symbol.Subject;

import java.util.Objects;
import java.util.stream.Stream;

public sealed interface SimpleEditAction extends EditAction permits AdditionAction, DeletionAction {

  default Subject getSubject() {
    var candidates = Stream.of(getOldSubject(), getNewSubject())
      .filter(Objects::nonNull)
      .toList();
    if (candidates.size() != 1) {
      throw new IllegalStateException("getOnlySubject() may only be called on single-subject EditAction types");
    }
    return candidates.getFirst();
  }

}
