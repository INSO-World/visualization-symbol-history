package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.property.value.TypeParameter;

import java.util.List;

@PropertyKey("typeParameters")
public record TypeParametersProperty(List<TypeParameter> value) implements ListProperty<TypeParameter> {

}
