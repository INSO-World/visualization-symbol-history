package com.mategka.dava.analyzer.struct.property.value;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Optional;

@Getter
@AllArgsConstructor(staticName = "of")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@EqualsAndHashCode
public final class KnownType implements Type {

  long id;

  @Override
  public Optional<Long> getKnownTypeId() {
    return Optional.of(id);
  }

  @Override
  public String toString() {
    return String.valueOf(id);
  }

}
