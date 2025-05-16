package com.mategka.dava.analyzer.spoon.path;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;

import static com.mategka.dava.analyzer.spoon.path.SpoonPathConstants.K_NAME;
import static com.mategka.dava.analyzer.spoon.path.SpoonPathConstants.K_SIGNATURE;

public record Argument(@NotNull String key, @Nullable String value) {

  public static @NotNull Argument forElement(@NotNull CtElement element) {
    if (element instanceof CtExecutable<?> executable) {
      String signature = executable.getSignature();
      if (executable instanceof CtConstructor) {
        signature = signature.substring(signature.indexOf('('));
      }
      return new Argument(K_SIGNATURE, signature);
    }
    return new Argument(K_NAME, SpoonPaths.getName(element));
  }

}
