package com.mategka.dava.analyzer.serialization.model;

import com.mategka.dava.analyzer.struct.property.value.Kind;
import com.mategka.dava.analyzer.struct.property.value.Visibility;
import com.mategka.dava.analyzer.struct.property.value.type.Type;

import com.google.common.collect.Multimap;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.time.YearMonth;

@Value
public class IndexRootDto {

  // Name index is compiled at deserialization since it requires fuzzy search support

  @NonNull
  Multimap<@NotNull String, @NotNull Long> byTrigrams;

  @NonNull
  Multimap<@NotNull Visibility, @NotNull Long> byVisibility;

  @NonNull
  Multimap<@NotNull Kind, @NotNull Long> byKind;

  @NonNull
  Multimap<@NotNull Type, @NotNull Long> byType;

  @NonNull
  Multimap<@NotNull YearMonth, @NotNull Long> byExistence;

  @NonNull
  Multimap<@NotNull YearMonth, @NotNull Long> byChanged;

}
