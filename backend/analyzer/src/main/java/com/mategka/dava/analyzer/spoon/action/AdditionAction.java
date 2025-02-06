package com.mategka.dava.analyzer.spoon.action;

import com.mategka.dava.analyzer.struct.symbol.Subject;

import lombok.NonNull;
import lombok.Value;

@Value(staticConstructor = "of")
public class AdditionAction implements EditAction {

  Subject oldSubject = null;

  @NonNull
  Subject newSubject;

}
