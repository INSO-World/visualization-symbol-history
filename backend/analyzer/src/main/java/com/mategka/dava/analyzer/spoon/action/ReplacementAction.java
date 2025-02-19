package com.mategka.dava.analyzer.spoon.action;

import com.mategka.dava.analyzer.extension.PairStream;
import com.mategka.dava.analyzer.struct.symbol.Subject;

import lombok.NonNull;
import lombok.Value;

@Value(staticConstructor = "of")
public class ReplacementAction implements EditAction {

  @NonNull
  Subject oldSubject;

  @NonNull
  Subject newSubject;

  static ReplacementAction fromTuple(ReplacementTuple tuple) {
    var oldSubject = tuple.deletion().action().getOldSubject();
    var newSubject = tuple.addition().action().getNewSubject();
    return ReplacementAction.of(oldSubject, newSubject);
  }

  @Override
  public String toString() {
    return PairStream.of(oldSubject, newSubject)
      .mapBoth(Subject::toDescriptor)
      .mapReduce("R %s -> %s"::formatted)
      .findFirst()
      .orElseThrow();
  }

}
