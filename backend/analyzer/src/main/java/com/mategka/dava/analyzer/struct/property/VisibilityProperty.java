package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import spoon.reflect.declaration.ModifierKind;

import java.util.Arrays;
import java.util.Optional;

@PropertyKey("visibility")
public record VisibilityProperty(Visibility value) implements SimpleProperty<VisibilityProperty.Visibility> {

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

    public VisibilityProperty toProperty() {
      return new VisibilityProperty(this);
    }

  }

  @Override
  public String toString() {
    return value.getPseudoKeyword();
  }

}
