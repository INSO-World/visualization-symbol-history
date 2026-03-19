package com.mategka.dava.analyzer.serialization.model;

import lombok.Value;

import java.util.List;

@Value(staticConstructor = "of")
public class OriginDto {

  int parentIndex;
  int sourceCommit;

  public static List<OriginDto> listOf(int parentIndex, int sourceCommit) {
    return List.of(OriginDto.of(parentIndex, sourceCommit));
  }

}
