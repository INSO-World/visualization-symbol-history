package com.mategka.dava.analyzer.struct.property.index;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyKey {

  @NotNull String value();

}
