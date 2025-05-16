package com.mategka.dava.analyzer.spoon.path;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spoon.reflect.CtModelImpl;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.meta.RoleHandler;
import spoon.reflect.meta.impl.RoleHandlerHelper;
import spoon.reflect.path.CtElementPathBuilder;
import spoon.reflect.path.CtPathException;
import spoon.reflect.path.CtRole;

import java.util.*;

import static com.mategka.dava.analyzer.spoon.path.SpoonPathConstants.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public final class SpoonPathElement {

  final TreeMap<String, String> arguments = new TreeMap<>();

  @Getter
  CtRole role;

  /**
   * Retrieve the path element for a given {@link CtElement}.
   * This method corresponds to {@link CtElementPathBuilder#fromElement(CtElement, CtElement)}, except that it does not
   * iterate over parent elements. This allows for the (client-side) caching of path elements, speeding up lookup.
   *
   * @param element the element for which to retrieve corresponding path element
   * @throws CtPathException if the relationship between the given element and its parent is unset, no handler for an
   *                         existing relationship type could be found, its parent element does not have the given element as a child, or the
   *                         given element lacks a name in a context where only named elements should be able to occur
   */
  @Contract("_ -> new")
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
      case SINGLE -> {
      }
      case LIST -> {
        var argument = Argument.forElement(element);
        List<CtElement> list = roleHandler.asList(parent);
        if (argument.value() != null) {
          if (!role.getSubRoles().isEmpty()) {
            pathElement.resetWithRole(role.getMatchingSubRoleFor(element));
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
            pathElement.addArgument(K_INDEX, String.valueOf(index));
          }
        } else {
          int index = 0;
          for (Object o : list) {
            if (o == element) {
              break;
            }
            index++;
          }
          pathElement.addArgument(K_INDEX, String.valueOf(index));
        }
      }
      case SET -> {
        String name = SpoonPaths.getName(element);
        if (name == null) {
          throw new CtPathException();
        }
        pathElement.addArgument(K_NAME, name);
      }
      case MAP -> {
        Map<String, Object> map = roleHandler.asMap(parent);
        String key = map.entrySet().stream()
          .filter(e -> e.getValue() == element)
          .map(Map.Entry::getKey)
          .findFirst()
          .orElseThrow(CtPathException::new);
        pathElement.addArgument(K_KEY, key);
      }
    }
    return pathElement;
  }

  public void addArgument(@NotNull String key, @Nullable String value) {
    arguments.put(key, value);
  }

  public void addArgument(@NotNull Argument argument) {
    arguments.put(argument.key(), argument.value());
  }

  public void removeArgument(@NotNull String key) {
    arguments.remove(key);
  }

  public void resetWithRole(CtRole role) {
    this.role = role;
    arguments.clear();
  }

  @Override
  public String toString() {
    if (role == null) {
      return SpoonPaths.ROOT_PACKAGE_PATH;
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

}
