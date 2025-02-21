package com.mategka.dava.analyzer.struct.property.value;

public record Expression(String string) {

  @Override
  public String toString() {
    return string;
  }

}
