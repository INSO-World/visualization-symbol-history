package com.mategka.dava.analyzer.extension;

import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import lombok.experimental.UtilityClass;

import java.util.EnumSet;
import java.util.HashMap;

@UtilityClass
public class MultimapsX {

  public <K, V extends Enum<V>> SetMultimap<K, V> newEnumSetMultimap(Class<V> enumClass) {
    return Multimaps.newSetMultimap(new HashMap<>(), () -> EnumSet.noneOf(enumClass));
  }

}
