package com.mategka.dava.analyzer.extension;

import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public class MapsX {

  public static <K1, K2, V> V get(Map<? super K1, K2> map1, Map<? super K2, V> map2, K1 key) {
    if (!map1.containsKey(key)) {
      return null;
    }
    return map2.get(map1.get(key));
  }

  public static <K1, K2, K3, V> V get(Map<? super K1, K2> map1, Map<? super K2, K3> map2, Map<? super K3, V> map3,
                                      K1 key) {
    if (!map1.containsKey(key)) {
      return null;
    }
    var key2 = map1.get(key);
    if (!map2.containsKey(key2)) {
      return null;
    }
    return map3.get(map2.get(key2));
  }

}
