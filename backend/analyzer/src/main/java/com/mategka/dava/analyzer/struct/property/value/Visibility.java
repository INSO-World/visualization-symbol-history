package com.mategka.dava.analyzer.struct.property.value;

import com.mategka.dava.analyzer.extension.AnStream;
import com.mategka.dava.analyzer.extension.ComparatorsX;
import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.struct.property.VisibilityProperty;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.ModifierKind;
import spoon.support.reflect.CtExtendedModifier;

import java.util.Objects;

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

  public static Visibility fromModifiable(CtModifiable modifiable) {
    return AnStream.from(modifiable.getExtendedModifiers())
      .sorted(ComparatorsX.falseFirst(CtExtendedModifier::isImplicit))
      .map(CtExtendedModifier::getKind)
      .filter(Objects::nonNull)
      .map(Visibility::fromModifierKind)
      .mapMulti(Option.yieldIfSome())
      .findFirstAsOption()
      .getOrElse(PACKAGE_PRIVATE);
  }

  public static Option<Visibility> fromModifierKind(ModifierKind modifier) {
    return AnStream.from(values())
      .filter(v -> v.spoonKind == modifier)
      .findFirstAsOption();
  }

  public VisibilityProperty toProperty() {
    return new VisibilityProperty(this);
  }

}
