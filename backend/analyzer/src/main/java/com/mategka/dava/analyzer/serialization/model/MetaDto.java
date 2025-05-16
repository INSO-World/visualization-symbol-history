package com.mategka.dava.analyzer.serialization.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class MetaDto {

  @NonNull
  String name;

  @NonNull
  ZonedDateTime createdAt;

  @NonNull
  ZonedDateTime updatedAt;

  @NonNull
  ZonedDateTime indexedAt;

  int commitCount;
  long strandSymbolCount;
  long symbolCount;
  int strandCount;

}
