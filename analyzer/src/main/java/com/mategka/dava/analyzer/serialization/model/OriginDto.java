package com.mategka.dava.analyzer.serialization.model;

import lombok.Value;

@Value(staticConstructor = "of")
public class OriginDto {

  int parentIndex;
  int sourceCommit;

}
