package com.mategka.dava.analyzer.spoon.action;

import com.mategka.dava.analyzer.extension.CollectionsX;
import com.mategka.dava.analyzer.extension.CollectorsX;
import com.mategka.dava.analyzer.extension.MyStream;
import com.mategka.dava.analyzer.extension.option.Option;

import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;

import java.util.Collection;

record ReplacementTuple(ActionIndex<DeletionAction> deletion, ActionIndex<AdditionAction> addition) {

  static Option<ReplacementTuple> find(Collection<EditAction> rawActions) {
    var actionsToIndices = MyStream.fromIndexed(rawActions).collect(CollectorsX.pairsToMap());
    var candidates = MyStream.from(rawActions)
      .narrow(SimpleEditAction.class)
      .filter(a -> a.getReferenceParent() instanceof CtPackage)
      .filter(a -> a.getReferenceElement() instanceof CtType<?>)
      .toList();
    var deletion = CollectionsX.firstOfType(candidates, DeletionAction.class);
    var addition = CollectionsX.lastOfType(candidates, AdditionAction.class);
    if (deletion.isNone() || addition.isNone()) {
      return Option.None();
    }
    var deletionAction = deletion.map(d -> new ActionIndex<>(d, actionsToIndices.get(d))).getOrThrow();
    var additionAction = addition.map(a -> new ActionIndex<>(a, actionsToIndices.get(a))).getOrThrow();
    return Option.Some(new ReplacementTuple(deletionAction, additionAction));
  }

  record ActionIndex<T extends EditAction>(T action, int index) {

  }

}
