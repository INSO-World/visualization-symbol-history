package com.mategka.dava.analyzer.serialization.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Locale;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(doNotUseGetters = true)
public enum EventFlag {
  NONE(EventCategory.MINISCULE),
  ADDED(EventCategory.ADDED),
  DELETED(EventCategory.DELETED),
  BRANCHED(EventCategory.MINOR),
  REPLACED(EventCategory.MAJOR),
  VALUE(EventCategory.MINOR),
  ANNOTATIONS(EventCategory.MAJOR),
  KIND(EventCategory.MAJOR),
  MOVED(EventCategory.MAJOR),
  RENAMED(EventCategory.MAJOR),
  REORDERED(EventCategory.MINOR),
  BODY(EventCategory.MINOR),
  MODIFIERS(EventCategory.MAJOR),
  REALIZATIONS(EventCategory.MAJOR),
  SUPERTYPES(EventCategory.MAJOR),
  TYPE_PARAMETERS(EventCategory.MAJOR),
  TYPE(EventCategory.MAJOR),
  VISIBILITY(EventCategory.MAJOR),
  ;

  EventCategory category;

  @JsonValue
  public String getJsonValue() {
    return name().toLowerCase(Locale.ROOT);
  }

}
