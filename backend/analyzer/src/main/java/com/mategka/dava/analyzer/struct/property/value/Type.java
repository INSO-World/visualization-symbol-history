package com.mategka.dava.analyzer.struct.property.value;

import com.mategka.dava.analyzer.extension.option.Option;

public sealed interface Type permits KnownType, UnknownType {

  default Option<Long> getKnownTypeId() {
    return Option.None();
  }

  default Option<String> getUnknownTypeName() {
    return Option.None();
  }

}
