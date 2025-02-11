package com.mategka.dava.analyzer.spoon.action;

import com.mategka.dava.analyzer.struct.symbol.Subject;

public sealed interface EditAction
  permits DeepUpdateAction, MoveAction, ReplacementAction, SimpleEditAction, UpdateAction {

  Subject getOldSubject();

  Subject getNewSubject();

}
