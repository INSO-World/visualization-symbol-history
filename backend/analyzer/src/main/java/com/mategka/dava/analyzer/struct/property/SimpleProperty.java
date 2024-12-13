package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.impl.SimpleNameProperty;
import org.jetbrains.annotations.NotNull;

public sealed interface SimpleProperty<T> extends Property permits SimpleNameProperty {

  @NotNull T value();

}
