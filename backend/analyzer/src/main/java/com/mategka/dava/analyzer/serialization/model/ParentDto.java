package com.mategka.dava.analyzer.serialization.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

@Value
public class ParentDto {

  @JsonValue
  Long id;
  long stateIndex;

}
