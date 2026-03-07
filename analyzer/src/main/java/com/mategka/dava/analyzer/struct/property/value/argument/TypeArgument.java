package com.mategka.dava.analyzer.struct.property.value.argument;

import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;
import java.util.Map;

public sealed interface TypeArgument extends Serializable permits ConcreteTypeArgument, WildcardTypeArgument {

  @JsonValue
  Map<String, Object> toJsonValue();

}
