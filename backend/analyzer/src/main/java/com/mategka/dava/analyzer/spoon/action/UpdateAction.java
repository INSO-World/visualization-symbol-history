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

}
