package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import spoon.reflect.declaration.*;

import java.util.Locale;

@PropertyKey("kind")
public record KindProperty(Value value) implements EnumProperty<KindProperty.Value> {

  public static KindProperty fromType(CtType<?> type) {
    return Value.fromType(type).toProperty();
  }

  @Override
  public String toString() {
    return value.toKeyword();
  }

  public enum Value {
    PACKAGE,
    CLASS,
    RECORD,
    INTERFACE,
    ENUM,
    FIELD,
    METHOD,
    VARIABLE,
    PARAMETER,
    CONSTANT,
    ENUM_CONSTANT,
    ANNOTATION,
    CONSTRUCTOR,
    ;

    public static Value fromType(CtType<?> type) {
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

    public String toKeyword() {
      return name().toLowerCase(Locale.ROOT);
    }

  }

}
