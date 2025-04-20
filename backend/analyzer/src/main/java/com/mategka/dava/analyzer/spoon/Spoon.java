package com.mategka.dava.analyzer.spoon;

import com.mategka.dava.analyzer.extension.option.Options;

import com.github.gumtreediff.tree.Tree;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileSystem;
import org.jetbrains.annotations.NotNull;
import spoon.reflect.CtModelImpl;
import spoon.reflect.declaration.*;

@UtilityClass
public class Spoon {

  public final String METADATA_KEY = "spoon_object";

  public String descriptorOf(CtElement element) {
    return "%s %s".formatted(
      simpleNameOf(element.getClass()),
      Options.cast(element, CtNamedElement.class)
        .map(CtNamedElement::getSimpleName)
        .getOrElse("(unnamed)")
    );
  }

  public String filePathOf(CtCompilationUnit unit) {
    return FileSystem.LINUX.normalizeSeparators(unit.getFile().getPath());
  }

  public CtElement getMetaElement(@NotNull Tree tree) {
    return (CtElement) tree.getMetadata(METADATA_KEY);
  }

  public boolean isDefaultConstructor(CtConstructor<?> constructor) {
    return !constructor.isCompactConstructor() && constructor.isImplicit() && constructor.getParameters().isEmpty();
  }

  public boolean isRegularConstructor(CtConstructor<?> constructor) {
    return !isDefaultConstructor(constructor) && !constructor.isCompactConstructor();
  }

  public boolean isRootPackage(CtPackage pakkage) {
    return pakkage instanceof CtModelImpl.CtRootPackage;
  }

  public String simpleNameOf(Class<? extends CtElement> clazz) {
    var name = clazz.getSimpleName();
    if (name.startsWith("Ct")) {
      name = name.substring(2);
    }
    if (name.endsWith("Impl")) {
      name = name.substring(0, name.length() - 4);
    }
    return name;
  }

}
