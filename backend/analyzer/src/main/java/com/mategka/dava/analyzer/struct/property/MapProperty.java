package com.mategka.dava.analyzer.struct.property;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public non-sealed interface MapProperty<K, V> extends Property {

  @NotNull Map<K, V> value();

}
