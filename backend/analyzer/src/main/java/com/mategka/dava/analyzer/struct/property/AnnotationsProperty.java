package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;

import java.util.List;

@PropertyKey("annotations")
public record AnnotationsProperty(List<? extends TypeValue> value) implements ListProperty<TypeValue> {

}
