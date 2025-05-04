package com.mategka.dava.analyzer.serialization.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.time.YearMonth;
import java.util.List;
import java.util.SequencedMap;

@Value
@Builder
public class SymbolDto {

  long id;

  boolean deleted;

  @NonNull
  List<@NotNull KeyDto> keys;

  @NonNull
  SequencedMap<@NotNull YearMonth, List<@NotNull StateDto>> states;

}
