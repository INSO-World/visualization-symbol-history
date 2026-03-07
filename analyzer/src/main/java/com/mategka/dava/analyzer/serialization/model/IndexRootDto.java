package com.mategka.dava.analyzer.serialization.model;

import com.mategka.dava.analyzer.struct.property.value.Kind;
import com.mategka.dava.analyzer.struct.property.value.Visibility;

import com.google.common.collect.Multimap;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.time.YearMonth;

@Value
@Builder
public class IndexRootDto {

  // Name index is compiled at deserialization since it requires fuzzy search support

  @NonNull
  Multimap<@NotNull Visibility, @NotNull Long> byVisibility;

  @NonNull
  Multimap<@NotNull Kind, @NotNull Long> byKind;

  @NonNull
  Multimap<@NotNull String, @NotNull Long> byType;

  @NonNull
  Multimap<@NotNull YearMonth, @NotNull Long> byExistence;

  @NonNull
  Multimap<@NotNull YearMonth, @NotNull Long> byChanged;

}
