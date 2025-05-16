package com.mategka.dava.analyzer.struct.property;

import java.io.Serializable;

public sealed interface SerializableProperty<T extends Serializable> extends TypedProperty<T>
  permits EnumProperty, NullableProperty, SimpleProperty {

}
