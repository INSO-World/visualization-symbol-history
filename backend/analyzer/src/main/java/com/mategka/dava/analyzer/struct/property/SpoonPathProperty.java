package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.spoon.path.SpoonPaths;
import com.mategka.dava.analyzer.struct.property.index.PropertyKey;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import spoon.reflect.declaration.CtElement;

import java.util.IdentityHashMap;

@PropertyKey("spoonPath")
public record SpoonPathProperty(String value) implements StringProperty {

  public static final SpoonPathProperty ROOT = new SpoonPathProperty(SpoonPaths.ROOT_PACKAGE_PATH);

  @Contract("_, _ -> new")
  public static @NotNull SpoonPathProperty fromElement(@NotNull CtElement element,
                                                       @NotNull IdentityHashMap<CtElement, String> memo) {
    return new SpoonPathProperty(SpoonPaths.getPath(element, memo));
  }

  @Override
  public String toString() {
    return value;
  }

}
