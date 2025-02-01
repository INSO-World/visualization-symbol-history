package com.mategka.dava.analyzer.struct;

import com.mategka.dava.analyzer.struct.property.index.PropertyIndexable;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;
import com.mategka.dava.analyzer.struct.symbol.Symbol;

import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

@Value
public class SymbolUpdate implements PropertyIndexable {

  long id;

  long strandId;

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
    return id == symbol.getId() && strandId == symbol.getStrandId();
  }

  public enum Flag {
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
