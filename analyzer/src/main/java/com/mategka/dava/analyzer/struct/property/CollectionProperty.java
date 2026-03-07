package com.mategka.dava.analyzer.struct.property;

import java.io.Serializable;
import java.util.Collection;

public sealed interface CollectionProperty<T extends Serializable> extends TypedProperty<Collection<T>>
  permits ListProperty, SetProperty {

}
