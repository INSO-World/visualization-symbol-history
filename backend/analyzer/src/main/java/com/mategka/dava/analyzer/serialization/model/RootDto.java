package com.mategka.dava.analyzer.serialization.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Value
@Builder
public class RootDto {

  @NonNull
  List<@NotNull CommitDto> commits;

  @NonNull
  List<@NotNull SymbolDto> symbols;

  //@NonNull
  //IndexRootDto indices;

}
