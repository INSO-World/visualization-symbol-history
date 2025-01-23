package com.mategka.dava.analyzer;

import com.mategka.dava.analyzer.git.CommitOrder;
import com.mategka.dava.analyzer.git.FileChangeType;
import com.mategka.dava.analyzer.git.RepositoryWrapper;
import com.mategka.dava.analyzer.git.Side;
import com.mategka.dava.analyzer.struct.History;
import com.mategka.dava.analyzer.struct.StrandWorkspace;
import com.mategka.dava.analyzer.struct.SymbolCreationContext;
import com.mategka.dava.analyzer.struct.property.*;
import com.mategka.dava.analyzer.struct.symbol.Symbol;
import com.mategka.dava.analyzer.util.*;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.jetbrains.annotations.NotNull;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtBodyHolder;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.declaration.*;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.compiler.VirtualFile;
import spoon.support.reflect.CtExtendedModifier;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App {

  public static void main(String[] args) {
    System.out.println("Hello World!");
    // ?REPO
    // ?REPO
    try (RepositoryWrapper repository = RepositoryWrapper.open("?REPO")) {
      Ref mainBranch = repository.resolveRef("main").orElseThrow();
      var history = History.emptyOfBranch(repository, mainBranch);
      var timeBefore = System.currentTimeMillis();
      var symbolIdCounter = new AtomicLong();
      int offset = 0;
      var comparator = new AstComparator();
      Map<Long, StrandWorkspace> workspaces = new DefaultMap<>(HashMap::new, StrandWorkspace::new);
      Map<VirtualFile, CtCompilationUnit> filesToUnits = new HashMap<>();
      // TODO: Traverse commits in normal topological order for ~5% performance boost
      try (RevWalk walk = repository.commitsUpTo(mainBranch, CommitOrder.REVERSE_TOPOLOGICAL)) {
        for (RevCommit commit : walk) {
          var commitSha = commit.getId().getName();
          var strandId = history.getStrandMapping().get(commitSha).getId();
          System.out.print(commitSha.substring(0, 6) + " ");
          if (++offset >= 12) {
            offset = 0;
            System.out.println();
          }
          var parent = Optionals.getFirst(commit.getParents());
          if (parent.isEmpty()) {
            var diffs = repository.initialCommitFilesOf(commit);
            var relevantDiffs = selectRelevantChanges(diffs);
            var additions = relevantDiffs.get(FileChangeType.ADDED);
            if (additions.isEmpty()) {
              // No relevant changes
              continue;
            }
            var currentContents = workspaces.get(strandId).getFiles();
            for (var diff : additions) {
              var content = repository.readFile(diff, Side.NEW).getSuccess().orElseThrow();
              var file = new VirtualFile(content, diff.getNewPath());
              currentContents.put(file.getPath(), file);
            }
            for (var file : currentContents.values()) {
              filesToUnits.put(file, Spoon.parse(file));
            }
            // TODO: Process symbol additions
            continue;
          }
          var actualParent = parent.get();
          var parentStrandId = history.getStrandMapping().get(actualParent.getId().getName()).getId();
          var parentContents = workspaces.get(parentStrandId).getFiles();
          try (DiffFormatter formatter = repository.newFormatter()) {
            var diffs = formatter.scan(actualParent.getTree(), commit.getTree());
            var relevantDiffs = selectRelevantChanges(diffs);
            Map<String, VirtualFile> overrideFiles = getOverrides(relevantDiffs, repository);
            if (overrideFiles.isEmpty()) {
              // No relevant changes
              continue;
            }
            Map<String, VirtualFile> effectiveFiles = new ChainMap<>(overrideFiles, parentContents);
            Map<VirtualFile, CtCompilationUnit> newUnits = effectiveFiles.values().stream()
              .filter(Objects::nonNull)
              .collect(Collectors2.toMap(Spoon::parse));
            // TODO: Do not trust rename, move and copy hints from Git
            var derivativeDiffPairs = Stream.of(FileChangeType.RENAMED, FileChangeType.MOVED, FileChangeType.COPIED)
              .flatMap(t -> relevantDiffs.get(t).stream().map(d -> Pair.of(t, d)))
              .toList();
            var creationContext = new SymbolCreationContext(symbolIdCounter, commitSha);
            for (var diffPair : derivativeDiffPairs) {
              var type = diffPair.getLeft();
              var diff = diffPair.getRight();
              // Treat declared type as renamed symbol
              var oldUnit = filesToUnits.get(parentContents.get(diff.getOldPath()));
              var newUnit = newUnits.get(overrideFiles.get(diff.getNewPath()));
              var astDiff = comparator.compare(oldUnit.getMainType(), newUnit.getMainType());
              var editScript = astDiff.getRootOperations();
              var mappings = astDiff.getMappingsComp();
              int dummy = 1;
            }
            for (var diff : relevantDiffs.get(FileChangeType.MODIFIED)) {
              var oldUnit = filesToUnits.get(parentContents.get(diff.getOldPath()));
              var newUnit = newUnits.get(overrideFiles.get(diff.getNewPath()));
              var astDiff = comparator.compare(oldUnit.getMainType(), newUnit.getMainType());
              var editScript = astDiff.getRootOperations();
              var mappings = astDiff.getMappingsComp();
              int dummy = 1;
            }
            for (var diff : relevantDiffs.get(FileChangeType.ADDED)) {
              var newUnit = newUnits.get(overrideFiles.get(diff.getNewPath()));
              var packageDeclaration = newUnit.getPackageDeclaration().getReference().getQualifiedName();
              var pakkage = workspaces.get(strandId).getPackage(packageDeclaration, creationContext);
              var typeDeclaration = newUnit.getMainType();
              var addedSymbols = addTypeDeclaration(typeDeclaration, pakkage, creationContext);
              int dummy = 1;
            }
            for (var diff : relevantDiffs.get(FileChangeType.DELETED)) {
              var oldUnit = filesToUnits.get(parentContents.get(diff.getOldPath()));
              var typeDeclaration = oldUnit.getMainType();
              int dummy = 1;
            }
            // TODO: Diff compilation units before and after
            // TODO: Process symbol changes
          }
        }
      }
      System.out.println("Done!");
      var time = System.currentTimeMillis() - timeBefore;
      System.out.println("Time (ms): " + time);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    // Goal 1: Get all changes for one commit
    // Goal 2: Get all changes for history (starting from specified commit)
    // Goal 3: Get all changes and refactorings for history
    // Goal 4: Get all symbol changes for history
    // Goal 5: Get all structured symbol changes for history (symbol parentage, ...)
    // Side Goal: Make sure each commit-file combo is only read and parsed ONCE

    /*
    Procedure:
    1) Get all file changes
    2) For added files, parse the contents and add all symbols (package symbols may already exist)
    3) For deleted files, mark all symbols associated with the file as deleted (recurs. delete packages if empty now)
    4) For modified files, parse the contents, retrieve the previous parsed contents, diff, then add/remove accordingly
    5) For moved and renamed files, proceed as with modifications, then:
    5a) If no semantic changes (only package and import changes): "true" move or rename (only move or rename)
    5b) If semantic changes: move/rename + add/remove symbols accordingly
    5z) Add new package symbols if applicable, recursively delete package symbols if empty now
    6) For copied files, proceed as with modifications, then:
    6a) If no semantic changes: "true" copy (simply copy currently known symbols, add package if applicable)
    6b) If semantic changes: treat new file like an addition (add symbols, add package if applicable)
     */
  }

  private static EnumMap<FileChangeType, List<DiffEntry>> selectRelevantChanges(Collection<DiffEntry> diffs) {
    var result = new EnumMap<FileChangeType, List<DiffEntry>>(FileChangeType.class);
    for (FileChangeType type : FileChangeType.values()) {
      result.put(type, new ArrayList<>());
    }
    for (var diff : diffs) {
      var changeType = diff.getChangeType();
      switch (changeType) {
        case ADD, MODIFY, COPY -> {
          if (App.isFileRelevant(diff.getNewPath())) {
            result.get(FileChangeType.fromJGitChangeType(changeType)).add(diff);
          }
        }
        case DELETE -> {
          if (App.isFileRelevant(diff.getOldPath())) {
            result.get(FileChangeType.DELETED).add(diff);
          }
        }
        case RENAME -> {
          var oldPathIsRelevant = App.isFileRelevant(diff.getOldPath());
          var newPathIsRelevant = App.isFileRelevant(diff.getNewPath());
          if (oldPathIsRelevant && newPathIsRelevant) {
            if (areSiblingPaths(diff.getOldPath(), diff.getNewPath())) {
              result.get(FileChangeType.RENAMED).add(diff);
            } else {
              result.get(FileChangeType.MOVED).add(diff);
            }
          } else if (oldPathIsRelevant) {
            result.get(FileChangeType.DELETED).add(diff);
          } else if (newPathIsRelevant) {
            result.get(FileChangeType.ADDED).add(diff);
          }
        }
      }
    }
    return result;
  }

  private static Map<String, VirtualFile> getOverrides(
    Map<FileChangeType, List<DiffEntry>> diffs,
    RepositoryWrapper repository
  ) {
    return diffs.entrySet().stream()
      .flatMap(e -> e.getValue().stream().map(d -> Pair.of(e.getKey(), d)))
      .flatMap(p -> {
        List<Pair<String, VirtualFile>> entries = new ArrayList<>();
        var type = p.getLeft();
        var diff = p.getRight();
        if (type.isRemovingOldResource()) {
          entries.add(Pair.of(diff.getOldPath(), null));
        }
        if (type.isAddingNewResource()) {
          var newContent = repository.readFile(diff, Side.NEW).getSuccess().orElseThrow();
          var newFile = new VirtualFile(newContent, diff.getNewPath());
          entries.add(Pair.of(diff.getNewPath(), newFile));
        }
        return entries.stream();
      })
      .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
  }

  private static boolean isFileRelevant(@NotNull String filename) {
    return filename.endsWith(".java");
  }

  private static boolean areSiblingPaths(@NotNull String path1, @NotNull String path2) {
    return Path.of(path1).getParent().equals(Path.of(path2).getParent());
  }

  private static VisibilityProperty.Visibility getVisibility(CtModifiable modifiable) {
    return modifiable.getExtendedModifiers().stream()
      .sorted(Streams.falseFirst(CtExtendedModifier::isImplicit))
      .map(CtExtendedModifier::getKind)
      .filter(Objects::nonNull)
      .map(VisibilityProperty.Visibility::fromModifierKind)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .findFirst()
      .orElse(VisibilityProperty.Visibility.PACKAGE_PRIVATE);
  }

  private static List<Symbol> addSymbol(CtElement element, Symbol parent, SymbolCreationContext context) {
    if (element instanceof CtType<?> type) {
      return addTypeDeclaration(type, parent, context);
    } else if (element instanceof CtConstructor<?> constructor) {
      if (!isDefaultConstructor(constructor) && !constructor.isCompactConstructor()) {
        return addConstructor(constructor, parent, context);
      }
    } else if (element instanceof CtParameter<?> parameter) {
      return addParameter(parameter, parent, context);
    } else if (element instanceof CtEnumValue<?> enumConstant) {
      return addEnumConstant(enumConstant, parent, context);
    } else if (element instanceof CtField<?> field) {
      return addField(field, parent, context);
    } else if (element instanceof CtMethod<?> method) {
      return addMethod(method, parent, context);
    } else if (element instanceof CtLocalVariable<?> variable) {
      return addVariable(variable, parent, context);
    }
    return Collections.emptyList();
  }

  private static List<Symbol> addTypeDeclaration(CtType<?> typeDeclaration, Symbol parent, SymbolCreationContext context) {
    var visibility = getVisibility(typeDeclaration);
    var members = typeDeclaration.getTypeMembers();
    var symbol = commonSymbolBuilder(context, typeDeclaration)
      .property(KindProperty.fromType(typeDeclaration))
      .property(new ParentProperty(parent))
      .property(visibility.toProperty())
      .build();
    return Lists.cons(
      symbol,
      Lists.flatMap(members, m -> addSymbol(m, symbol, context))
    );
  }

  private static List<Symbol> addConstructor(CtConstructor<?> constructor, Symbol parent, SymbolCreationContext context) {
    var name = parent.getProperty(SimpleNameProperty.class).value();
    var visibility = getVisibility(constructor);
    var variables = constructor.getBody().getElements(new TypeFilter<CtVariable<?>>(CtVariable.class));
    var symbol = commonSymbolBuilder(context, constructor)
      .property(KindProperty.Value.CONSTRUCTOR.toProperty())
      .property(new ParentProperty(parent))
      .property(new SimpleNameProperty(name))
      .property(visibility.toProperty())
      .build();
    return Lists.cons(
      symbol,
      Lists.flatMap(constructor.getParameters(), p -> addSymbol(p, symbol, context)),
      Lists.flatMap(variables, v -> addSymbol(v, symbol, context))
    );
  }

  private static List<Symbol> addMethod(CtMethod<?> method, Symbol parent, SymbolCreationContext context) {
    var visibility = getVisibility(method);
    var variables = Optional.ofNullable(method.getBody())
      .map(b -> b.getElements(new TypeFilter<CtVariable<?>>(CtVariable.class)))
      .orElseGet(Collections::emptyList);
    var symbol = commonSymbolBuilder(context, method)
      .property(KindProperty.Value.METHOD.toProperty())
      .property(new ParentProperty(parent))
      .property(visibility.toProperty())
      .build();
    return Lists.cons(
      symbol,
      Lists.flatMap(method.getParameters(), p -> addSymbol(p, symbol, context)),
      Lists.flatMap(variables, v -> addSymbol(v, symbol, context))
    );
  }

  private static List<Symbol> addParameter(CtParameter<?> parameter, Symbol parent, SymbolCreationContext context) {
    var symbol = commonSymbolBuilder(context, parameter)
      .property(KindProperty.Value.PARAMETER.toProperty())
      .property(new ParentProperty(parent))
      .build();
    return List.of(symbol);
  }

  private static List<Symbol> addEnumConstant(CtEnumValue<?> enumConstant, Symbol parent, SymbolCreationContext context) {
    var arguments = Optional.ofNullable(enumConstant.getDefaultExpression())
      .map(i -> (CtConstructorCall<?>) i)
      .map(CtAbstractInvocation::getArguments)
      .orElseGet(Collections::emptyList);
    assert parent.getProperty(KindProperty.class).value() == KindProperty.Value.ENUM;
    var symbol = commonSymbolBuilder(context, enumConstant)
      .property(KindProperty.Value.ENUM_CONSTANT.toProperty())
      .property(new ParentProperty(parent))
      .build();
    return List.of(symbol);
  }

  private static List<Symbol> addField(CtField<?> field, Symbol parent, SymbolCreationContext context) {
    var modifiers = getModifiers(field);
    var kind = modifiers.containsAll(ModifiersProperty.Modifier.CONSTANT_FIELD_MODIFIERS)
      ? KindProperty.Value.CONSTANT
      : KindProperty.Value.FIELD;
    var initialValue = Optional.ofNullable(field.getDefaultExpression());
    var symbol = commonSymbolBuilder(context, field)
      .property(kind.toProperty())
      .property(new ParentProperty(parent))
      .build();
    return List.of(symbol);
  }

  private static List<Symbol> addVariable(CtLocalVariable<?> variable, Symbol parent, SymbolCreationContext context) {
    var modifiers = getModifiers(variable);
    var kind = modifiers.containsAll(ModifiersProperty.Modifier.CONSTANT_VARIABLE_MODIFIERS)
      ? KindProperty.Value.CONSTANT
      : KindProperty.Value.VARIABLE;
    var initialValue = Optional.ofNullable(variable.getDefaultExpression());
    var symbol = commonSymbolBuilder(context, variable)
      .property(kind.toProperty())
      .property(new ParentProperty(parent))
      .build();
    return List.of(symbol);
  }

  private static Symbol.SymbolBuilder commonSymbolBuilder(SymbolCreationContext context, CtElement element) {
    var builder = context.symbolBuilder()
      .property(new LineRangeProperty(getLineNumbers(element)))
      .property(new PathProperty(element.getPath()));
    if (element instanceof CtNamedElement namedElement) {
      builder.property(new SimpleNameProperty(namedElement.getSimpleName()));
    }
    if (element instanceof CtModifiable modifiable) {
      builder.property(new ModifiersProperty(getModifiers(modifiable)));
    }
    if (element instanceof CtTypedElement<?> typedElement) {
      // TODO: Replace with known type where applicable (may be generic type parameter!)
      builder.property(TypeProperty.unknownFromTypedElement(typedElement));
    }
    // TODO: Add type parameters (declarations) if applicable
    return builder;
  }

  private static List<CtElement> getVariables(CtBodyHolder element) {
    return element.getBody().getDirectChildren().stream().flatMap(childElement -> {
      if (childElement instanceof CtType<?>) {
        return Stream.empty();
      }
      if (childElement instanceof CtBodyHolder bodyHolder) {
        return getVariables(bodyHolder).stream();
      }
      if (childElement instanceof CtLocalVariable<?> localVariable) {
        return Stream.of(localVariable);
      }
      return Stream.empty();
    }).toList();
  }

  private static EnumSet<ModifiersProperty.Modifier> getModifiers(CtModifiable modifiable) {
    var visibility = modifiable.getVisibility();
    return modifiable.getExtendedModifiers().stream()
      .map(CtExtendedModifier::getKind)
      .filter(kind -> kind != visibility)
      .filter(Objects::nonNull)
      .map(ModifiersProperty.Modifier::fromModifierKind)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(Collectors.toCollection(() -> EnumSet.noneOf(ModifiersProperty.Modifier.class)));
  }

  private static Pair<Integer, Integer> getLineNumbers(CtElement element) {
    var position = element.getPosition();
    if (!position.isValidPosition()) {
      throw new IllegalStateException("Symbol element should have had valid position");
    }
    return Pair.of(position.getSourceStart(), position.getSourceEnd());
  }

  private static boolean isDefaultConstructor(CtConstructor<?> constructor) {
    return !constructor.isCompactConstructor() && constructor.isImplicit() && constructor.getParameters().isEmpty();
  }

}
