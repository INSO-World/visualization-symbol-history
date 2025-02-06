package com.mategka.dava.analyzer.spoon;

import com.mategka.dava.analyzer.util.JavaSyntax;

import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileSystem;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtConstructor;
import spoon.support.compiler.VirtualFile;

import java.util.List;

@UtilityClass
public class Spoon {

  public CtModel EMPTY_MODEL = newLauncher().getModel();

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
    return model.getAllModules()
      .stream()
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

  public static boolean isDefaultConstructor(CtConstructor<?> constructor) {
    return !constructor.isCompactConstructor() && constructor.isImplicit() && constructor.getParameters().isEmpty();
  }

  public static boolean isRegularConstructor(CtConstructor<?> constructor) {
    return !isDefaultConstructor(constructor) && !constructor.isCompactConstructor();
  }

}
