package com.mategka.dava.analyzer.serialization.model;

import com.mategka.dava.analyzer.struct.property.value.Kind;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;

@Data
@Builder
public final class KeyDto {

  @NonNull
  final ParentDto parent;
  @NonNull
  final ZonedDateTime from;
  @NonNull
  final String name;
  @NonNull
  final Kind kind;
  @Nullable
  @JsonInclude(JsonInclude.Include.NON_NULL)
  ZonedDateTime to;

}
