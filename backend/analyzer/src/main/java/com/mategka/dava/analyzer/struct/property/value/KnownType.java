package com.mategka.dava.analyzer.struct.property.value;

import com.mategka.dava.analyzer.extension.option.Option;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor(staticName = "of")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@EqualsAndHashCode
public final class KnownType implements Type {

  long id;

  @Override
  public Option<Long> getKnownTypeId() {
    return Option.Some(id);
  }

  @Override
  public String toString() {
    return String.valueOf(id);
  }

}
