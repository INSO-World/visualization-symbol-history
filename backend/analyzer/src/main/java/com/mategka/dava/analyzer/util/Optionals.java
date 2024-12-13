package com.mategka.dava.analyzer.util;

import lombok.experimental.UtilityClass;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@UtilityClass
public class Optionals {

  public <T> Optional<T> cast(Optional<? super T> optional, Class<T> targetType) {
    return optional.filter(targetType::isInstance).map(targetType::cast);
  }

}
