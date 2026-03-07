package com.mategka.dava.analyzer.struct.property.value;

import com.mategka.dava.analyzer.struct.property.KindProperty;

import spoon.reflect.declaration.*;

import java.util.Locale;

public enum Kind {
  // Organizational units
  MODULE,
  PACKAGE,
  // Classes
  CLASS,
  RECORD,
  ENUM,
  // Interfaces
  INTERFACE,
  ANNOTATION,
  // Field members
  FIELD,
  CONSTANT_FIELD,
  ENUM_CONSTANT,
  // Method members
  METHOD,
  CONSTRUCTOR,
  // Local variables
  PARAMETER,
  VARIABLE,
  CONSTANT_VARIABLE,
  ;

  public static Kind fromType(CtType<?> type) {
    if (type instanceof CtRecord) {
      return RECORD;
    }
    if (type instanceof CtEnum<?>) {
      return ENUM;
    }
    if (type instanceof CtClass<?>) {
      return CLASS;
    }
    if (type instanceof CtAnnotationType<?>) {
      return ANNOTATION;
    }
    if (type instanceof CtInterface<?>) {
      return INTERFACE;
    }
    throw new IllegalArgumentException("No compatible kind value for type " + type.getClass().getSimpleName());
  }

  public KindProperty toProperty() {
    return new KindProperty(this);
  }

  public String toPseudoKeyword() {
    return name().replace("_", " ").toLowerCase(Locale.ROOT);
  }

}
