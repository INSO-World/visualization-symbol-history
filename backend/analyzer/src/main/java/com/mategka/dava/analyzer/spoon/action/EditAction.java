package com.mategka.dava.analyzer.spoon.action;

public sealed interface EditAction
  permits MoveAction, ReplacementAction, SimpleEditAction, UpdateAction {

}
