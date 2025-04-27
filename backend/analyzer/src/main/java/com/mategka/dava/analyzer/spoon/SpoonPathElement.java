package com.mategka.dava.analyzer.spoon;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spoon.reflect.CtModelImpl;
import spoon.reflect.declaration.*;
import spoon.reflect.meta.RoleHandler;
import spoon.reflect.meta.impl.RoleHandlerHelper;
import spoon.reflect.path.CtPathException;
import spoon.reflect.path.CtRole;
import spoon.reflect.path.impl.CtRolePathElement;
import spoon.reflect.reference.CtReference;

import java.util.*;
import java.util.regex.Pattern;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public final class SpoonPathElement {

  private static final String PREFIX = CtRolePathElement.STRING;
  private static final String ARGUMENTS_START = CtRolePathElement.ARGUMENT_START;
  private static final String ARGUMENTS_END = CtRolePathElement.ARGUMENT_END;
  private static final String ARGUMENT_ASSIGN = CtRolePathElement.ARGUMENT_NAME_SEPARATOR;
  private static final String ARGUMENT_SEPARATOR = ";";

  public static final String ROOT_PACKAGE_PATH = "";
  static final Pattern INDEX_PATTERN = Pattern.compile("index\\s*=\\s*\\d+(?=[;\\]])");

  final TreeMap<String, String> arguments = new TreeMap<>();

  @Getter
  CtRole role;

  public static @NotNull String getPath(@NotNull CtElement element, @NotNull IdentityHashMap<CtElement, String> memo) {
    if (memo.containsKey(element)) {
      return memo.get(element);
    }
    if (Spoon.isRootPackage(element)) {
      return memo.computeIfAbsent(element, _e -> ROOT_PACKAGE_PATH);
    }
    var path = getPath(element.getParent(), memo) + forElement(element);
    memo.put(element, path);
    return path;
  }

  public static @NotNull SpoonPathElement forElement(@NotNull CtElement element) throws CtPathException {
    if (element instanceof CtModelImpl.CtRootPackage) {
      return new SpoonPathElement(null);
    }
    CtElement parent = element.getParent();
    CtRole role = element.getRoleInParent();
    if (role == null) {
      throw new CtPathException();
    }
    RoleHandler roleHandler = RoleHandlerHelper.getOptionalRoleHandler(parent.getClass(), role);
    if (roleHandler == null) {
      throw new CtPathException();
    }
    var pathElement = new SpoonPathElement(role);
    switch (roleHandler.getContainerKind()) {
      case SINGLE -> {}
      case LIST -> {
        var argument = Argument.forElement(element);
        List<CtElement> list = roleHandler.asList(parent);
        if (argument.value() != null) {
          if (!role.getSubRoles().isEmpty()) {
            pathElement.setRole(role.getMatchingSubRoleFor(element));
          }
          pathElement.addArgument(argument);
          int matchCount = 0;
          int index = -1;
          for (CtElement item : list) {
            if (item == element) {
              index = matchCount;
              matchCount++;
            } else if (argument.equals(Argument.forElement(item))) {
              matchCount++;
            }
          }
          if (matchCount > 1 && index >= 0) {
            pathElement.addArgument(Argument.KEY_INDEX, String.valueOf(index));
          }
        } else {
          int index = 0;
          for (Object o : list) {
            if (o == element) {
              break;
            }
            index++;
          }
          pathElement.addArgument(Argument.KEY_INDEX, String.valueOf(index));
        }
      }
      case SET -> {
        String name = getName(element);
        if (name == null) {
          throw new CtPathException();
        }
        pathElement.addArgument(Argument.KEY_NAME, name);
      }
      case MAP -> {
        Map<String, Object> map = roleHandler.asMap(parent);
        String key = map.entrySet().stream()
          .filter(e -> e.getValue() == element)
          .map(Map.Entry::getKey)
          .findFirst()
          .orElseThrow(CtPathException::new);
        pathElement.addArgument(Argument.KEY_KEY, key);
      }
    }
    return pathElement;
  }

  public static @NotNull String simplify(@NotNull String path) {
    return INDEX_PATTERN.matcher(path).replaceAll("");
  }

  public static @NotNull String getParentPath(@NotNull String path) {
    var result = simplify(path);
    var lastPoundIndex = result.lastIndexOf("#");
    result = result.substring(0, lastPoundIndex);
    return result;
  }

  private static @Nullable String getName(@NotNull CtElement element) {
    return switch (element) {
      case CtNamedElement namedElement -> namedElement.getSimpleName();
      case CtReference reference -> reference.getSimpleName();
      default -> null;
    };
  }

  public void addArgument(@NotNull String key, @Nullable String value) {
    arguments.put(key, value);
  }

  public void addArgument(@NotNull Argument argument) {
    arguments.put(argument.key, argument.value);
  }

  public void removeArgument(@NotNull String key) {
    arguments.remove(key);
  }

  public void setRole(CtRole role) {
    this.role = role;
    arguments.clear();
  }

  @Override
  public String toString() {
    if (role == null) {
      return ROOT_PACKAGE_PATH;
    }
    var argumentsString = "";
    if (!arguments.isEmpty()) {
      var argumentsStringJoiner = new StringJoiner(ARGUMENT_SEPARATOR, ARGUMENTS_START, ARGUMENTS_END);
      for (var argument : arguments.entrySet()) {
        argumentsStringJoiner.add(argument.getKey() + ARGUMENT_ASSIGN + argument.getValue());
      }
      argumentsString = argumentsStringJoiner.toString();
    }
    return PREFIX + role.toString() + argumentsString;
  }

  public record Argument(@NotNull String key, @Nullable String value) {

    public static final String KEY_NAME = "name";
    public static final String KEY_SIGNATURE = "signature";
    public static final String KEY_INDEX = "index";
    public static final String KEY_KEY = "key";

    public static @NotNull Argument forElement(@NotNull CtElement element) {
      if (element instanceof CtExecutable<?> executable) {
        String signature = executable.getSignature();
        if (executable instanceof CtConstructor) {
          signature = signature.substring(signature.indexOf('('));
        }
        return new Argument(KEY_SIGNATURE, signature);
      }
      return new Argument(KEY_NAME, getName(element));
    }

  }

}
