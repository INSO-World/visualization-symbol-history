package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.extension.option.Options;
import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.property.value.Modifier;

import spoon.reflect.declaration.CtModifiable;
import spoon.support.reflect.CtExtendedModifier;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@PropertyKey("modifiers")
public record ModifiersProperty(Set<Modifier> value) implements SetProperty<Modifier> {

  public static ModifiersProperty fromModifiable(CtModifiable modifiable) {
    return new ModifiersProperty(getModifiers(modifiable));
  }

  public static Set<Modifier> getModifiers(CtModifiable modifiable) {
    var visibility = modifiable.getVisibility();
    return AnStream.from(modifiable.getExtendedModifiers())
      .map(CtExtendedModifier::getKind)
      .filter(kind -> kind != visibility)
      .filter(Objects::nonNull)
      .map(Modifier::fromModifierKind)
      .mapMulti(Options.yieldIfSome())
      .collect(Collectors.toSet());
  }

  @Override
  public String toString() {
    return value.stream()
      .map(Modifier::toKeyword)
      .collect(Collectors.joining(" "));
  }

}
