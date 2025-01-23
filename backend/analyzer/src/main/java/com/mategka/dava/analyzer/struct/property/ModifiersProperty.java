package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import spoon.reflect.declaration.ModifierKind;

import java.util.*;
import java.util.stream.Collectors;

@PropertyKey("modifiers")
public record ModifiersProperty(EnumSet<Modifier> value) implements EnumSetProperty<ModifiersProperty.Modifier> {

  @Getter
  @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @ToString(doNotUseGetters = true)
  public enum Modifier {
    STATIC(ModifierKind.STATIC),
    SEALED(ModifierKind.SEALED),
    ABSTRACT(ModifierKind.ABSTRACT),
    FINAL(ModifierKind.FINAL),
    SYNCHRONIZED(ModifierKind.SYNCHRONIZED),
    TRANSIENT(ModifierKind.TRANSIENT),
    VOLATILE(ModifierKind.VOLATILE),
    ;

    public static final Collection<Modifier> CONSTANT_FIELD_MODIFIERS = List.of(STATIC, FINAL);
    public static final Collection<Modifier> CONSTANT_VARIABLE_MODIFIERS = List.of(FINAL);

    private ModifierKind spoonKind;

    public static Optional<Modifier> fromModifierKind(ModifierKind modifier) {
      return Arrays.stream(values())
        .filter(v -> v.spoonKind == modifier)
        .findFirst();
    }

    public String toKeyword() {
      return name().toLowerCase(Locale.ROOT);
    }

  }

  @Override
  public String toString() {
    return value.stream()
      .map(Modifier::toKeyword)
      .collect(Collectors.joining(" "));
  }

}
