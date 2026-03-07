package com.mategka.dava.analyzer.struct.property;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

public sealed interface ListProperty<T extends Serializable> extends CollectionProperty<T>
  permits AnnotationsProperty, EnumArgumentsProperty, RealizationsProperty, SupertypesProperty, TypeParametersProperty {

  @NotNull List<T> value();

}
