package com.mategka.dava.analyzer.serialization.model;

import com.mategka.dava.analyzer.struct.property.index.PropertyMap;
import com.mategka.dava.analyzer.struct.symbol.UpdateFlag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Value
@Builder
public class StateDto {

  @NonNull
  ChangeCause cause;

  @NonNull
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  List<OriginDto> origins;

  @NonNull
  Integer commit;

  long symbolId;

  @NonNull
  @Builder.Default
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  Set<@NotNull String> updated = Collections.emptySet();

  @NonNull
  @JsonIgnoreProperties(value = "spoonPath")
  PropertyMap properties;

  @NonNull
  @Builder.Default
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  Set<@NotNull UpdateFlag> flags = Collections.emptySet();

}
