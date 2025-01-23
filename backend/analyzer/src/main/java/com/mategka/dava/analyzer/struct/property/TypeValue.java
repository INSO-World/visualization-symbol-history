package com.mategka.dava.analyzer.struct.property;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Optional;

public sealed interface TypeValue permits TypeValue.KnownType, TypeValue.UnknownType {

  default Optional<Long> getKnownTypeId() {
    return Optional.empty();
  }

  default Optional<String> getUnknownTypeName() {
    return Optional.empty();
  }

  @Getter
  @AllArgsConstructor(staticName = "of")
  @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
  @EqualsAndHashCode
  final class KnownType implements TypeValue {

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

  @Getter
  @AllArgsConstructor(staticName = "of")
  @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
  @EqualsAndHashCode
  final class UnknownType implements TypeValue {

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

}
