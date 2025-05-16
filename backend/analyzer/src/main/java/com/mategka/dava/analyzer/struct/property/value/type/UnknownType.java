package com.mategka.dava.analyzer.struct.property.value.type;

import com.mategka.dava.analyzer.extension.CollectorsX;
import com.mategka.dava.analyzer.struct.property.value.argument.TypeArgument;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor(staticName = "of")
@RequiredArgsConstructor(staticName = "of")
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode
public final class UnknownType implements Type {

  @Serial
  private static final long serialVersionUID = 1302856928484918722L;

  @NonNull
  final String qualifiedName;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  List<TypeArgument> typeArguments = new ArrayList<>();

  @JsonIgnore
  private @NotNull String getSimpleName() {
    return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
  }

  @Override
  public @NotNull String toString() {
    return getSimpleName() + (typeArguments.isEmpty()
      ? ""
      : "<%s>".formatted(typeArguments.stream().collect(CollectorsX.commaSeparated()))
    );
  }

}
