package com.mategka.dava.analyzer.struct.property.value.type;

import com.mategka.dava.analyzer.extension.CollectorsX;
import com.mategka.dava.analyzer.struct.property.value.argument.TypeArgument;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor(staticName = "of")
@RequiredArgsConstructor(staticName = "of")
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode
public final class KnownType implements Type {

  @Serial
  private static final long serialVersionUID = 6779881295485194585L;

  final long symbolId;

  List<TypeArgument> typeArguments = new ArrayList<>();

  @Override
  public String toString() {
    return symbolId + (typeArguments.isEmpty()
      ? ""
      : "<%s>".formatted(typeArguments.stream().collect(CollectorsX.commaSeparated()))
    );
  }

}
