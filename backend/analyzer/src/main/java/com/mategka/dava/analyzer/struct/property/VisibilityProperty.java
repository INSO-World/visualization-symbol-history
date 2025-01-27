package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.extension.Streams;
import com.mategka.dava.analyzer.struct.property.index.PropertyKey;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.ModifierKind;
import spoon.support.reflect.CtExtendedModifier;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@PropertyKey("visibility")
public record VisibilityProperty(Visibility value) implements SimpleProperty<VisibilityProperty.Visibility> {

  @Override
  public String toString() {
    return value.getPseudoKeyword();
  }

  @Getter
  @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @ToString(doNotUseGetters = true)
  public enum Visibility {
    PUBLIC("public", ModifierKind.PUBLIC),
    PROTECTED("protected", ModifierKind.PROTECTED),
    PRIVATE("private", ModifierKind.PRIVATE),
    PACKAGE_PRIVATE("(package-private)", null),
    ;

    private String pseudoKeyword;

    private ModifierKind spoonKind;

    public static Optional<Visibility> fromModifierKind(ModifierKind modifier) {
      return Arrays.stream(values())
        .filter(v -> v.spoonKind == modifier)
        .findFirst();
    }

    public static Visibility fromModifiable(CtModifiable modifiable) {
      return modifiable.getExtendedModifiers().stream()
        .sorted(Streams.falseFirst(CtExtendedModifier::isImplicit))
        .map(CtExtendedModifier::getKind)
        .filter(Objects::nonNull)
        .map(Visibility::fromModifierKind)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst()
        .orElse(PACKAGE_PRIVATE);
    }

    public VisibilityProperty toProperty() {
      return new VisibilityProperty(this);
    }

  }

}
