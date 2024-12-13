package com.mategka.dava.analyzer.struct.property;

import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.function.Supplier;

@UtilityClass
class Properties {

  private final Map<Class<? extends Property>, Supplier<Object>> DEFAULTERS = Map.ofEntries(
    Map.entry(SimpleProperty.class, () -> null),
    Map.entry(OptionalProperty.class, Optional::empty),
    Map.entry(ListProperty.class, ArrayList::new),
    Map.entry(MapProperty.class, HashMap::new),
    Map.entry(SetProperty.class, HashSet::new)
  );

  Object getDefault(Class<? extends Property> propertyClass) {
    return DEFAULTERS.get(propertyClass).get();
  }

}
