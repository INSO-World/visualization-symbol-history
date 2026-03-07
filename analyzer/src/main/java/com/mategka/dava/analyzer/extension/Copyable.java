package com.mategka.dava.analyzer.extension;

import org.jetbrains.annotations.NotNull;

public interface Copyable<T> {

  @NotNull T copy();

}
