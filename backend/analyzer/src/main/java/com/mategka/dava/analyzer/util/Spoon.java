package com.mategka.dava.analyzer.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileSystem;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtCompilationUnit;

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

  public String pathOf(CtCompilationUnit unit) {
    return FileSystem.LINUX.normalizeSeparators(unit.getFile().getPath());
  }

}
