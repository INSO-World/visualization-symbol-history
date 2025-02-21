package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.struct.property.index.PropertyIndexable;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;

import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

@Value
public class SymbolUpdate implements PropertyIndexable {

  SymbolKey key;

  @NonNull
  String commitSha;

  @NonNull
  PropertyMap properties;

  int flags;

  public EnumSet<Flag> getFlagSet() {
    return Arrays.stream(Flag.values())
      .filter(f -> f.isSet(flags))
      .collect(Collectors.toCollection(() -> EnumSet.noneOf(Flag.class)));
  }

  public boolean appliesTo(@NotNull Symbol symbol) {
    return key.equals(symbol.getKey());
  }

  public enum Flag {
    REPLACED,
    RENAMED,
    MOVED,
    MOVED_WITH_PARENT,
    REORDERED,
    ;

    public int value() {
      return 1 << ordinal();
    }

    public boolean isSet(int flags) {
      return (flags & value()) != 0;
    }
  }

}
