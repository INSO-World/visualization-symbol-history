package com.mategka.dava.analyzer.spoon.action;

import com.mategka.dava.analyzer.extension.PairStream;
import com.mategka.dava.analyzer.spoon.Spoon;
import com.mategka.dava.analyzer.struct.symbol.Subject;

import lombok.NonNull;
import lombok.Value;

@Value(staticConstructor = "of")
public class ReplacementAction implements EditAction {

  @NonNull
  Subject oldSubject;

  @NonNull
  Subject newSubject;

  @Override
  public String toString() {
    return PairStream.of(oldSubject, newSubject)
      .mapBoth(Subject::getElement)
      .mapBoth(Spoon::descriptorOf)
      .mapReduce("R %s -> %s"::formatted)
      .findFirst()
      .orElseThrow();
  }

}
