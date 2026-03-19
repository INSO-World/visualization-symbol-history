package com.mategka.dava.analyzer.spoon;

import com.mategka.dava.analyzer.extension.stream.AnStream;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import spoon.compiler.Environment;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.support.compiler.VirtualFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class Launcher {

  private static final JavaSyntax DEFAULT_LANGUAGE_LEVEL = JavaSyntax.LTS21;

  spoon.Launcher launcher = new spoon.Launcher();

  @SuppressWarnings("unused")
  public Launcher() {
    this(DEFAULT_LANGUAGE_LEVEL);
  }

  public Launcher(JavaSyntax syntax) {
    applyDefaultSettings();
    setComplianceLevel(syntax);
  }

  public static CtCompilationUnit parse(VirtualFile virtualFile) throws CompilationException {
    return parse(virtualFile, DEFAULT_LANGUAGE_LEVEL);
  }

  public static CtCompilationUnit parse(VirtualFile virtualFile, JavaSyntax syntax) throws CompilationException {
    return new Launcher(syntax).parseFile(virtualFile);
  }

  private static List<? extends CtCompilationUnit> getCompilationUnits(CtModel model) {
    return AnStream.from(model.getAllModules())
      .flatMap(module -> module.getFactory().CompilationUnit().getMap().values().stream())
      .filter(unit -> unit.getUnitType() == CtCompilationUnit.UNIT_TYPE.TYPE_DECLARATION)
      .toList();
  }

  public CtCompilationUnit parseFile(VirtualFile virtualFile) throws CompilationException {
    try {
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
    } catch (Exception e) {
      // FUTURE: Incrementally upgrade parser until file compiles; only throw at LATEST level
      throw new CompilationException("Could not compile file: " + virtualFile.getName(), e);
    }
  }

  private void applyDefaultSettings() {
    launcher.addProcessor(new CtCaseHotfixProcessor());
    var env = launcher.getEnvironment();
    env.setNoClasspath(true);
    env.setPrettyPrintingMode(Environment.PRETTY_PRINTING_MODE.FULLYQUALIFIED);
    env.setShouldCompile(false);
    env.setTabulationSize(4);
    env.setEncoding(StandardCharsets.UTF_8);
    env.setPreserveLineNumbers(false);
    env.setCommentEnabled(false); // ignore comments to avoid linking problems and redundant body update flags
    env.disableConsistencyChecks(); // Avoid defensive programming for a slight performance boost
    // env.setLevel(Level.OFF.name());
  }

  private void setComplianceLevel(JavaSyntax syntax) {
    launcher.getEnvironment().setComplianceLevel(syntax.getToVersion());
  }

}
