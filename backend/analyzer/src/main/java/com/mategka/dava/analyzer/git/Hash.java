package com.mategka.dava.analyzer.git;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;

public record Hash(String full) {

  private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder().withoutPadding();

  @Contract(pure = true)
  public @NotNull String abbreviated() {
    return full.substring(0, 7);
  }

  @Contract(pure = true)
  public @NotNull String minimal() {
    byte[] bytes = new byte[3];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) Integer.parseInt(full.substring(2 * i, 2 * (i + 1)), 16);
    }
    return BASE64_ENCODER.encodeToString(bytes);
  }

  @Override
  public String toString() {
    return full;
  }

}
