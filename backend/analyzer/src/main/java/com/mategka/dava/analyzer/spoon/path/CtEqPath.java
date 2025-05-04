package com.mategka.dava.analyzer.spoon.path;

import com.mategka.dava.analyzer.extension.CollectionsX;
import com.mategka.dava.analyzer.extension.option.Option;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import spoon.reflect.CtModel;
import spoon.reflect.CtModelImpl;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.path.CtPath;
import spoon.reflect.path.CtPathException;
import spoon.reflect.path.impl.CtPathImpl;

import java.util.Objects;

/**
 * @deprecated Use {@link SpoonPathElement} instead.
 */
@Deprecated
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(staticName = "of")
public class CtEqPath implements Comparable<CtEqPath> {

  public static final CtEqPath EMPTY = CtEqPath.of(new CtPathImpl());

  CtPath path;

  @Getter(lazy = true, value = AccessLevel.PRIVATE)
  String cachedStringRepresentation = path.toString();

  public static CtEqPath of(@NotNull CtElement element) throws CtPathException {
    if (element instanceof CtModelImpl.CtRootPackage) {
      return EMPTY;
    }
    return of(element.getPath());
  }

  public <T extends CtElement> Option<T> evaluateOn(CtModel model, Class<T> clazz) {
    return CollectionsX.firstOfType(path.evaluateOn(model.getRootPackage()), clazz);
  }

  public String getPseudoParentString() {
    var result = toUnorderedString();
    var lastPoundIndex = result.lastIndexOf("#");
    result = result.substring(0, lastPoundIndex);
    return result;
  }

  public boolean isEmpty() {
    return this == EMPTY;
  }

  public String toUnorderedString() {
    return SpoonPaths.INDEX_PATTERN.matcher(getCachedStringRepresentation()).replaceAll("");
  }

  @Override
  public int compareTo(@NotNull CtEqPath o) {
    return toString().compareTo(o.toString());
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof CtEqPath ctPath)) return false;
    return Objects.equals(toString(), ctPath.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public String toString() {
    return getCachedStringRepresentation();
  }

}
