package com.mategka.dava.analyzer.serialization.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.SequencedMap;

@Value
@Builder
public class SymbolDto {

  long id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  ZonedDateTime deletedAt;

  @NonNull
  List<@NotNull KeyDto> keys;

  @NonNull
  SequencedMap<@NotNull YearMonth, List<@NotNull StateDto>> states;

}
