package com.mategka.dava.analyzer.serialization.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ContributionDto {

  int author;

  int percent;

}
