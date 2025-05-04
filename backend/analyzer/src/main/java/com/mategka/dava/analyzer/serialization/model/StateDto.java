package com.mategka.dava.analyzer.serialization.model;

import com.mategka.dava.analyzer.struct.property.index.PropertyMap;
import com.mategka.dava.analyzer.struct.symbol.SymbolKey;
import com.mategka.dava.analyzer.struct.symbol.UpdateFlag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

@Value
@Builder
public class StateDto {

  @NonNull
  ChangeCause cause;

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

  public static class StateDtoBuilder {

    public StateDtoBuilder key(SymbolKey key) {
      return symbolId(key.symbolId()).strand(key.strandId());
    }

  }

}
