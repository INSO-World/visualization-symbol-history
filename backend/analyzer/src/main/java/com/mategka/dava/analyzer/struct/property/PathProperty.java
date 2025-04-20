package com.mategka.dava.analyzer.struct.property;

import com.mategka.dava.analyzer.struct.property.index.PropertyKey;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

@PropertyKey("path")
public record PathProperty(String value) implements SimpleProperty<String> {

  public static final Pattern INDEX_PATTERN = Pattern.compile("index\\s*=\\s*\\d+(?=.*?])");

  public static PathProperty fromCtPathProperty(@NotNull CtPathProperty ctPathProperty) {
    return new PathProperty(INDEX_PATTERN.matcher(ctPathProperty.value().toString()).replaceAll(""));
  }

  @Override
  public String toString() {
    return value;
  }

}
