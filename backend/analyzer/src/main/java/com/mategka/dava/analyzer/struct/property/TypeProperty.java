package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Optional;

@PropertyKey("type")
public record TypeProperty(Value value) implements SimpleProperty<TypeProperty.Value> {

  public sealed interface Value permits KnownType, UnknownType {

    default Optional<Long> getKnownTypeId() {
      return Optional.empty();
    }

    default Optional<String> getUnknownTypeName() {
      return Optional.empty();
    }

  }

  @Getter
  @AllArgsConstructor(staticName = "of")
  @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
  @ToString
  @EqualsAndHashCode
  public static final class KnownType implements Value {

    long id;

    @Override
    public Optional<Long> getKnownTypeId() {
      return Optional.of(id);
    }

  }

  @Getter
  @AllArgsConstructor(staticName = "of")
  @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
  @ToString
  @EqualsAndHashCode
  public static final class UnknownType implements Value {

    String qualifiedName;

    @Override
    public Optional<String> getUnknownTypeName() {
      return Optional.of(qualifiedName);
    }

  }

}
