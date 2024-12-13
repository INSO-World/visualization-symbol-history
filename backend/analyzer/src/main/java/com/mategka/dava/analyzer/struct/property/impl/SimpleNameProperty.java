package com.mategka.dava.analyzer.struct.property.impl;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.property.SimpleProperty;

@PropertyKey("simpleName")
public record SimpleNameProperty(String value) implements SimpleProperty<String> { }
