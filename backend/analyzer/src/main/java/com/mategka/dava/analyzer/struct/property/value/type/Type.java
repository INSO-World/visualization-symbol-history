package com.mategka.dava.analyzer.struct.property.value.type;

import com.mategka.dava.analyzer.struct.property.value.argument.TypeArgument;

import java.util.List;

public sealed interface Type permits KnownType, UnknownType {

  List<TypeArgument> getTypeArguments();

}
