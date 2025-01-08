package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;

@PropertyKey("simpleName")
public record SimpleNameProperty(String value) implements SimpleProperty<String> { }
