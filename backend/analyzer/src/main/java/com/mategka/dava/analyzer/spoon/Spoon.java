package com.mategka.dava.analyzer.spoon;

import com.mategka.dava.analyzer.extension.Streamer;
import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.util.JavaSyntax;

import com.github.gumtreediff.tree.Tree;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileSystem;
import org.jetbrains.annotations.NotNull;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.*;
import spoon.support.compiler.VirtualFile;

import java.util.List;

@UtilityClass
public class Spoon {

  public final String METADATA_KEY = "spoon_object";

  public final CtModel EMPTY_MODEL = newLauncher().getModel();

  public Launcher newLauncher() {
    return newLauncher(JavaSyntax.LTS17);
  }

  public Launcher newLauncher(JavaSyntax syntax) {
    Launcher launcher = new Launcher();
    launcher.getEnvironment().setNoClasspath(true);
    launcher.getEnvironment().setComplianceLevel(syntax.getToVersion());
    return launcher;
  }

  public List<CtCompilationUnit> getCompilationUnits(CtModel model) {
    return Streamer.ofCollection(model.getAllModules())
      .flatMap(module -> module.getFactory().CompilationUnit().getMap().values().stream())
      .filter(unit -> unit.getUnitType() == CtCompilationUnit.UNIT_TYPE.TYPE_DECLARATION)
      .map(element -> (CtCompilationUnit) element)
      .toList();
  }

  public CtCompilationUnit parse(VirtualFile virtualFile) {
    var launcher = newLauncher();
    launcher.addInputResource(virtualFile);
    var model = launcher.buildModel();
    var units = getCompilationUnits(model);
    if (units.isEmpty()) {
      throw new IllegalStateException("Virtual file contains no compilation unit");
    }
    if (units.size() > 1) {
      throw new IllegalStateException("Virtual file contains more than one compilation unit");
    }
    return units.getFirst();
  }

  public String filePathOf(CtCompilationUnit unit) {
    return FileSystem.LINUX.normalizeSeparators(unit.getFile().getPath());
  }

  public boolean isDefaultConstructor(CtConstructor<?> constructor) {
    return !constructor.isCompactConstructor() && constructor.isImplicit() && constructor.getParameters().isEmpty();
  }

  public boolean isRegularConstructor(CtConstructor<?> constructor) {
    return !isDefaultConstructor(constructor) && !constructor.isCompactConstructor();
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

  public String descriptorOf(CtElement element) {
    return "%s %s".formatted(
      simpleNameOf(element.getClass()),
      Option.cast(element, CtNamedElement.class)
        .map(CtNamedElement::getSimpleName)
        .getOrElse("(unnamed)")
    );
  }

  public CtElement getMetaElement(@NotNull Tree tree) {
    return (CtElement) tree.getMetadata(METADATA_KEY);
  }

}
