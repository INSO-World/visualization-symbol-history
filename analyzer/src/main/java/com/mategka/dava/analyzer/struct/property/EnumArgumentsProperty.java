package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.extension.CollectorsX;
import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.property.value.Expression;

import java.util.List;

@PropertyKey("enumArguments")
public record EnumArgumentsProperty(List<Expression> value) implements ListProperty<Expression> {

  @Override
  public String toString() {
    return "(%s)".formatted(value.stream().collect(CollectorsX.commaSeparated()));
  }

}
