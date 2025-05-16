package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.struct.property.Property;
import com.mategka.dava.analyzer.struct.property.index.PropertyIndexable;

import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class SymbolUpdate implements PropertyIndexable {

  int parentIndex;

  @NonNull
  Context sourceContext;

  @NonNull
  Context targetContext;

  @NonNull
  Map<String, @Nullable Property> properties;

  @NonNull
  Set<UpdateFlag> flags;

  @Override
  public String toString() {
    var oldId = sourceContext.key().symbolId();
    var newId = targetContext.key().symbolId();
    return "%s [%s] %s".formatted(
      (oldId == newId ? String.valueOf(oldId) : "%d -> %d".formatted(oldId, newId)),
      flags.stream()
        .map(Objects::toString)
        .collect(Collectors.joining(", ")),
      properties
    );
  }

}
