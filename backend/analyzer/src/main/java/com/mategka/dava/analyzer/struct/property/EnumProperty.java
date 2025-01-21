package com.mategka.dava.analyzer.struct.property;

public sealed interface EnumProperty<T extends Enum<T>> extends SimpleProperty<T> permits KindProperty {

}
