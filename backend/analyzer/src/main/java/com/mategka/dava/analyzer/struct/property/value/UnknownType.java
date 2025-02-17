package com.mategka.dava.analyzer.struct.property.value;

import com.mategka.dava.analyzer.extension.option.Option;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor(staticName = "of")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@EqualsAndHashCode
public final class UnknownType implements Type {

  String qualifiedName;

  @Override
  public Option<String> getUnknownTypeName() {
    return Option.Some(qualifiedName);
  }

  @Override
  public String toString() {
    return qualifiedName;
  }

}
