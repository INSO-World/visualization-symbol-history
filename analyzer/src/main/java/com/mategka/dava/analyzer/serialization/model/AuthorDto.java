package com.mategka.dava.analyzer.serialization.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class AuthorDto {

  int id;

  @NonNull
  String name;

  @NonNull
  String email;

}
