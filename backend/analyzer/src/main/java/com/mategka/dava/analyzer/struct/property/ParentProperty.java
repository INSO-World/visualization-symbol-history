package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;

@PropertyKey("parent")
public record ParentProperty(Long value) implements SimpleProperty<Long> {

}
