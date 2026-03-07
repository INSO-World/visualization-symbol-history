package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.option.Options;
import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.property.value.Flag;

import org.jetbrains.annotations.NotNull;
import spoon.reflect.declaration.CtElement;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@PropertyKey("flags")
public record FlagsProperty(Set<Flag> value) implements SetProperty<Flag> {

  private static final Set<Flag> EMPTY = Collections.emptySet();
  private static final Set<Flag> IMPLICIT = Set.of(Flag.IMPLICIT);

  public static @NotNull Option<FlagsProperty> fromElement(@NotNull CtElement element) {
    return Options.fromSized(getFlags(element)).map(FlagsProperty::new);
  }

  private static Set<Flag> getFlags(@NotNull CtElement element) {
    return element.isImplicit() ? IMPLICIT : EMPTY;
  }

  @Override
  public String toString() {
    return value.stream()
      .map(Enum::name)
      .collect(Collectors.joining(", "));
  }

}
