package com.mategka.dava.analyzer.serialization.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Collection;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(doNotUseGetters = true)
public enum EventCategory {
  MINISCULE(1),
  MINOR(2),
  MAJOR(3),
  ADDED(4),
  DELETED(5),
  ;

  int value;

  @JsonValue
  public int getJsonValue() {
    return value;
  }

  public static EventCategory max(Collection<? extends EventFlag> events) {
    return events.stream().map(EventFlag::getCategory).reduce(MINISCULE, EventCategory::max);
  }

  public static EventCategory max(EventCategory a, EventCategory b) {
    return a.value < b.value ? b : a;
  }

}
