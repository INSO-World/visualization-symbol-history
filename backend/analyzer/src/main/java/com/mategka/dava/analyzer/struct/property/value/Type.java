package com.mategka.dava.analyzer.struct.property.value;

import java.util.Optional;

public sealed interface Type permits KnownType, UnknownType {

  default Optional<Long> getKnownTypeId() {
    return Optional.empty();
  }

  default Optional<String> getUnknownTypeName() {
    return Optional.empty();
  }

}
