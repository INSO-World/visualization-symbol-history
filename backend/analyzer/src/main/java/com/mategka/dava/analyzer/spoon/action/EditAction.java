package com.mategka.dava.analyzer.spoon.action;

import com.mategka.dava.analyzer.struct.symbol.Subject;

public sealed interface EditAction permits AdditionAction, DeepUpdateAction, DeletionAction, MoveAction, UpdateAction {

  Subject getOldSubject();

  Subject getNewSubject();

}
