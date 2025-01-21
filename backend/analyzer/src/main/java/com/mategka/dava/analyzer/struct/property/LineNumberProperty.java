package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;

@PropertyKey("line")
public record LineNumberProperty(Integer value) implements SimpleProperty<Integer> {

}
