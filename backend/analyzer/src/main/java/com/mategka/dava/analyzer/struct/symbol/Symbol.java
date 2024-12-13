package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.struct.CommitSha;
import com.mategka.dava.analyzer.struct.SymbolUpdate;
import com.mategka.dava.analyzer.struct.property.Property;
import com.mategka.dava.analyzer.struct.property.index.PropertyIndexable;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Value
@Builder(toBuilder = true)
public class Symbol implements PropertyIndexable {

  long id;

  @NonNull
  CommitSha commit;

  @NonNull
  @Builder.Default
  List<Long> predecessors = new ArrayList<>();

  @NonNull
  @Builder.Default
  PropertyMap properties = new PropertyMap();

  public static Symbol squash(Symbol base, Symbol current) {
    return current.toBuilder().id(base.id).build();
  }

  public PropertyMap diff(@NotNull Collection<Property> newProperties) {
    return newProperties.stream()
      .filter(p -> Optional.of(p.getKey())
        .map(properties::get)
        .map(Property::value)
        .map(v -> !v.equals(p.value()))
        .orElse(false)
      )
      .collect(PropertyMap.collectProperties());
  }

  public Symbol withUpdate(@NotNull SymbolUpdate update) {
    if (!Objects.equals(id, update.getId())) {
      throw new IllegalArgumentException(
        "Cannot apply update for symbol %d to symbol %d".formatted(update.getId(), id)
      );
    }
    return toBuilder().update(update).build();
  }

  public Symbol withUpdates(@NotNull Collection<? extends SymbolUpdate> updates) {
    var violation = updates.stream().map(SymbolUpdate::getId).filter(i -> i != id).findFirst();
    if (violation.isPresent()) {
      throw new IllegalArgumentException(
        "Cannot apply update for symbol %d to symbol %d".formatted(violation.get(), id)
      );
    }
    var updatedProperties = updates.stream()
      .map(SymbolUpdate::getProperties)
      .map(Map::entrySet)
      .flatMap(Collection::stream)
      .collect(PropertyMap.collectEntries());
    return toBuilder().properties(updatedProperties).build();
  }

  public static class SymbolBuilder {

    public SymbolBuilder predecessor(long id) {
      return predecessors(Collections.singletonList(id));
    }

    public SymbolBuilder property(@NonNull Property property) {
      return properties(PropertyMap.of(property));
    }

    public SymbolBuilder update(@NonNull SymbolUpdate update) {
      return properties(update.getProperties());
    }

  }

}
