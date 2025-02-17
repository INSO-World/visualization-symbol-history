package com.mategka.dava.analyzer.spoon.action;

public sealed interface EditAction
  permits BodyUpdateAction, MoveAction, ReplacementAction, SimpleEditAction, UpdateAction {

}
