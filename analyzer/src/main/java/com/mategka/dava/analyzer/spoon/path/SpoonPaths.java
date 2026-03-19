package com.mategka.dava.analyzer.spoon.path;

import com.mategka.dava.analyzer.spoon.Spoon;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.reference.CtReference;

import java.util.IdentityHashMap;
import java.util.regex.Pattern;

import static com.mategka.dava.analyzer.spoon.path.SpoonPathConstants.*;

@UtilityClass
public class SpoonPaths {

  public final String ROOT_PACKAGE_PATH = "";
  public final Pattern INDEX_PATTERN = Pattern.compile("%s%s\\d+%s?".formatted(
    K_INDEX,
    ARGUMENT_ASSIGN,
    ARGUMENT_SEPARATOR
  ));

  public @Nullable String getName(@NotNull CtElement element) {
    return switch (element) {
      case CtNamedElement namedElement -> namedElement.getSimpleName();
      case CtReference reference -> reference.getSimpleName();
      default -> null;
    };
  }

  public @NotNull String getParentPath(@NotNull String path) {
    var result = simplify(path);
    var lastPoundIndex = result.lastIndexOf("#");
    result = result.substring(0, lastPoundIndex);
    return result;
  }

  public @NotNull String getPath(@NotNull CtElement element, @NotNull IdentityHashMap<CtElement, String> memo) {
    // FUTURE: Put memo into separate class
    if (memo.containsKey(element)) {
      return memo.get(element);
    }
    if (Spoon.isRootPackage(element)) {
      return memo.computeIfAbsent(element, _e -> ROOT_PACKAGE_PATH);
    }
    var path = getPath(element.getParent(), memo) + SpoonPathElement.forElement(element);
    memo.put(element, path);
    return path;
  }

  public @NotNull String simplify(@NotNull String path) {
    return INDEX_PATTERN.matcher(path).replaceAll("");
  }

}
