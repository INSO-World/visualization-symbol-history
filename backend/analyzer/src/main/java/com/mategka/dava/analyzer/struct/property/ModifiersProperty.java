package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.property.value.Modifier;

import spoon.reflect.declaration.CtModifiable;
import spoon.support.reflect.CtExtendedModifier;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@PropertyKey("modifiers")
public record ModifiersProperty(EnumSet<Modifier> value) implements EnumSetProperty<Modifier> {

  public static ModifiersProperty fromModifiable(CtModifiable modifiable) {
    return new ModifiersProperty(getModifiers(modifiable));
  }

  public static EnumSet<Modifier> getModifiers(CtModifiable modifiable) {
    var visibility = modifiable.getVisibility();
    return modifiable.getExtendedModifiers().stream()
      .map(CtExtendedModifier::getKind)
      .filter(kind -> kind != visibility)
      .filter(Objects::nonNull)
      .map(Modifier::fromModifierKind)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(Collectors.toCollection(() -> EnumSet.noneOf(Modifier.class)));
  }

  @Override
  public String toString() {
    return value.stream()
      .map(Modifier::toKeyword)
      .collect(Collectors.joining(" "));
  }

}
