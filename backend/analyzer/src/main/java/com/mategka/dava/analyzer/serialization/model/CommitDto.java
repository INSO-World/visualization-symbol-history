package com.mategka.dava.analyzer.serialization.model;

import com.mategka.dava.analyzer.git.Hash;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.util.List;

@Value
@Builder
public class CommitDto {

  @NonNull Integer id;
  @NonNull Long strand;
  @NonNull Hash hash;
  @NonNull ZonedDateTime date;
  @NonNull String summary;
  @NonNull @JsonInclude(JsonInclude.Include.NON_EMPTY) String desc;
  @NonNull List<@NotNull Integer> parents;

}
