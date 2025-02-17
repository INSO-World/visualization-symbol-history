package com.mategka.dava.analyzer.struct.property.value;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Optional;

@Getter
@AllArgsConstructor(staticName = "of")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@EqualsAndHashCode
public final class UnknownType implements Type {

  String qualifiedName;

  @Override
  public Optional<String> getUnknownTypeName() {
    return Optional.of(qualifiedName);
  }

  @Override
  public String toString() {
    return qualifiedName;
  }

}
