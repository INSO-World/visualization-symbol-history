package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;
import com.mategka.dava.analyzer.struct.property.value.AnalyzerLevel;

@PropertyKey("_level")
public record AnalyzerLevelProperty(AnalyzerLevel value) implements SimpleProperty<AnalyzerLevel> {

  public static final AnalyzerLevelProperty CURRENT = new AnalyzerLevelProperty(AnalyzerLevel.V1);

  @Override
  public String toString() {
    return value.toString();
  }

}
