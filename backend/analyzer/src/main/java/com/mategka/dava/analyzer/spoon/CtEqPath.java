package com.mategka.dava.analyzer.spoon;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.NotNull;
import spoon.reflect.CtModel;
import spoon.reflect.CtModelImpl;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.path.CtPath;
import spoon.reflect.path.CtPathException;
import spoon.reflect.path.impl.CtPathImpl;

import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor(staticName = "of")
public class CtEqPath implements CtPath, Comparable<CtPath> {

  public static final CtEqPath EMPTY = CtEqPath.of(new CtPathImpl());

  @Delegate
  private final CtPath path;

  private String cachedStringRepresentation = null;

  public static CtEqPath of(CtElement element) throws CtPathException {
    if (element instanceof CtModelImpl.CtRootPackage) {
      return EMPTY;
    }
    return of(element.getPath());
  }

  public <T extends CtElement> Optional<T> evaluateOn(CtModel model, Class<T> clazz) {
    return path.evaluateOn(model.getRootPackage()).stream()
      .filter(clazz::isInstance)
      .map(clazz::cast)
      .findFirst();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof CtPath ctPath)) return false;
    return Objects.equals(toString(), ctPath.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public String toString() {
    if (cachedStringRepresentation != null) {
      return cachedStringRepresentation;
    }
    return cachedStringRepresentation = path.toString();
  }

  @Override
  public int compareTo(@NotNull CtPath o) {
    return toString().compareTo(o.toString());
  }

}
