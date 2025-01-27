package com.mategka.dava.analyzer.util;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(doNotUseGetters = true)
public enum JavaSyntax {

  /**
   * Base level
   */
  V4(1, 4),
  /**
   * Enums
   */
  V8(5, 8),
  /**
   * Modules
   */
  V9(9, 9),
  /**
   * Local variable type inference (var)
   */
  V13(10, 13),
  /**
   * Record classes
   */
  V15(14, 15),
  /**
   * Sealed classes, compact record constructors
   */
  V20(16, 20),
  /**
   * String templates
   */
  V22(21, 22),
  /**
   * Module imports (first preview), primitive type patterns
   */
  V23(23, 23),
  /**
   * Placeholder for future versions
   */
  V24(24, 24),
  ;

  public static final JavaSyntax LTS8 = JavaSyntax.V8;
  public static final JavaSyntax LTS11 = JavaSyntax.V13;
  public static final JavaSyntax LTS17 = JavaSyntax.V20;
  public static final JavaSyntax LTS21 = JavaSyntax.V22;
  public static final JavaSyntax LATEST = JavaSyntax.V23;

  int fromVersion;
  int toVersion;

  public static JavaSyntax fromVersion(int version) {
    return Arrays.stream(values())
      .filter(v -> v.covers(version))
      .findFirst()
      .orElseThrow();
  }

  public boolean covers(int version) {
    return fromVersion <= version && version <= toVersion;
  }

}
