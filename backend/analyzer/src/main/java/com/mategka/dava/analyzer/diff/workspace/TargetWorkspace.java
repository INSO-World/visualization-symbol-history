package com.mategka.dava.analyzer.diff.workspace;

import com.mategka.dava.analyzer.collections.Array;
import com.mategka.dava.analyzer.collections.Mapping;
import com.mategka.dava.analyzer.collections.Stack;
import com.mategka.dava.analyzer.diff.file.FileChange;
import com.mategka.dava.analyzer.diff.file.FileMapping;
import com.mategka.dava.analyzer.diff.file.ParentFile;
import com.mategka.dava.analyzer.extension.CollectorsX;
import com.mategka.dava.analyzer.extension.ListsX;
import com.mategka.dava.analyzer.extension.option.None;
import com.mategka.dava.analyzer.extension.option.Options;
import com.mategka.dava.analyzer.extension.option.Some;
import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.extension.struct.Pair;
import com.mategka.dava.analyzer.extension.struct.TreeNode;
import com.mategka.dava.analyzer.git.Repository;
import com.mategka.dava.analyzer.git.Side;
import com.mategka.dava.analyzer.spoon.CompilationException;
import com.mategka.dava.analyzer.spoon.CtEqPath;
import com.mategka.dava.analyzer.spoon.Spoon;
import com.mategka.dava.analyzer.struct.pipeline.Symbolizer;
import com.mategka.dava.analyzer.struct.property.PathProperty;
import com.mategka.dava.analyzer.struct.property.SimpleNameProperty;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;
import com.mategka.dava.analyzer.struct.property.value.Kind;
import com.mategka.dava.analyzer.struct.symbol.Symbol;

import lombok.experimental.UtilityClass;
import org.eclipse.jgit.diff.DiffEntry;
import org.jetbrains.annotations.Nullable;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtPackage;
import spoon.support.compiler.VirtualFile;

import java.util.*;

@UtilityClass
public class TargetWorkspace {

  public SymbolWorkspace create(Array<SymbolWorkspace> parentWorkspaces, FileMapping fileMapping,
                                Repository repository) {
    final var targetRoot = getTargetRoot(parentWorkspaces);
    final Map<String, TreeNode<Symbol>> fileSymbols = new TreeMap<>();
    final Map<String, CtCompilationUnit> fileSpoonUnits = new TreeMap<>();
    final Array<Set<Symbol>> unchangedFromParent = Array.fromSupplier(parentWorkspaces.length, HashSet::new);
    Set<String> targetPathsToUnlink = new HashSet<>();
    for (var targetFilePath : fileMapping.getMappings().targets()) {
      // TODO: This deletion check should be redundant?
      if (targetFilePath == null) {
        continue; // Ignore deletions
      }
      var sourceMappings = fileMapping.getMappings().getByTarget(targetFilePath);
      var unchangedSources = AnStream.from(sourceMappings)
        .filter(Mapping::isStatic)
        .map(Mapping::source)
        .toTypedArray();
      // NOTE: Since the subtree is unchanged for all unchangedSources, we can take any one of them
      TargetEntry entryData = switch (Options.getFirst(unchangedSources)) {
        case Some<ParentFile> some -> {
          var file = some.getOrThrow();
          var parentWorkspace = parentWorkspaces.get(file.parentIndex());
          var fileTree = parentWorkspace.getFileSymbols().get(file.filePath());
          if (fileTree == null) {
            // If a file is changed but does not exist in the parent workspace, it must be a failed strict addition
            if (unchangedSources.size() == sourceMappings.size()) {
              // If there are no changes made, we can safely skip the file as compilation would just fail again
              yield null;
            }
            var changeMetadata = AnStream.from(sourceMappings)
              .filter(m -> !m.isStatic())
              .map(Mapping::metadata)
              .map(FileChange::diffEntry)
              .findFirstAsOption()
              .getOrThrow();
            yield getNewlyParsedTargetEntry(parentWorkspaces, repository, targetFilePath, changeMetadata, sourceMappings, targetRoot);
          }
          var parentPackage = establishPackageHierarchyByName(targetRoot, fileTree);
          var spoonUnit = parentWorkspace.getFileSpoonUnits().get(file.filePath());
          yield new TargetEntry(parentPackage, fileTree.copy(), spoonUnit);
        }
        case None<?> _n -> {
          var changeMetadata = sourceMappings.getFirst().metadata().diffEntry(); // must exist since target exists
          yield getNewlyParsedTargetEntry(parentWorkspaces, repository, targetFilePath, changeMetadata, sourceMappings, targetRoot);
        }
      };
      if (entryData == null) {
        // Target will count as deleted
        targetPathsToUnlink.add(targetFilePath);
        continue;
      }
      for (var file : unchangedSources) {
        for (var node : entryData.fileTree()) {
          unchangedFromParent.get(file.parentIndex()).add(node.value());
        }
      }
      entryData.packageNode().add(entryData.fileTree());
      fileSymbols.put(targetFilePath, entryData.fileTree());
      fileSpoonUnits.put(targetFilePath, entryData.spoonUnit());
    }
    targetPathsToUnlink.forEach(fileMapping::unlinkTarget);
    Map<CtEqPath, TreeNode<Symbol>> locatedSymbols = targetRoot.stream()
      .map(Pair.fromRight(n -> n.value().getPath()))
      .collect(CollectorsX.pairsToMutableMap(TreeMap::new));
    return new SymbolWorkspace(targetRoot, fileSymbols, fileSpoonUnits, locatedSymbols, unchangedFromParent);
  }

  private static @Nullable TargetEntry getNewlyParsedTargetEntry(Array<SymbolWorkspace> parentWorkspaces,
                                                              Repository repository, String targetFilePath,
                                                              DiffEntry changeMetadata,
                                                              List<Mapping<ParentFile, String, FileChange>> sourceMappings,
                                                              TreeNode<Symbol> targetRoot) {
    var newContents = repository.readFile(changeMetadata, Side.NEW).getSuccess().orElseThrow();
    var virtualFile = new VirtualFile(newContents, targetFilePath);
    CtCompilationUnit spoonUnit = null;
    try {
      spoonUnit = Spoon.parse(virtualFile);
    } catch (CompilationException e) {
      e.printStackTrace();
      // TODO: Figure out better last resort error handling than taking first valid parent's state
      var nonAdditionChangeSource = AnStream.from(sourceMappings)
        // Mapping cannot be static, otherwise the parent would be uncompilable, too
        .filter(m -> !m.isAddition() && !m.isStatic())
        .map(Mapping::source)
        // If file tree does not exist in parent, this is a repeated compilation failure despite changes, try again later
        .filter(file -> parentWorkspaces.get(file.parentIndex()).getFileSymbols().containsKey(file.filePath()))
        .findFirstAsOption();
      return switch (nonAdditionChangeSource) {
        case Some<ParentFile> some -> {
          var file = some.getOrThrow();
          var parentWorkspace = parentWorkspaces.get(file.parentIndex());
          var fileTree = parentWorkspace.getFileSymbols().get(file.filePath()); // must exist by above precondition
          var parentPackage = establishPackageHierarchyByName(targetRoot, fileTree);
          spoonUnit = parentWorkspace.getFileSpoonUnits().get(file.filePath());
          yield new TargetEntry(parentPackage, fileTree.copy(), spoonUnit);
        }
        // If we get here, the file is a strict addition, just skip it, and try again after further changes
        case None<?> _n2 -> null;
      };
    }
    var parentPackage = establishPackageHierarchyByPath(targetRoot, spoonUnit);
    var fileTree = Symbolizer.symbolizeFileType(spoonUnit.getMainType(), parentPackage.value());
    return new TargetEntry(parentPackage, fileTree, spoonUnit);
  }

  private TreeNode<Symbol> establishPackageHierarchyByName(TreeNode<Symbol> targetRoot, TreeNode<Symbol> fileNode) {
    var packageStack = new Stack<Symbol>();
    var currentPackageNode = fileNode.parent().getOrThrow();
    while (!currentPackageNode.isRoot()) {
      packageStack.push(currentPackageNode.value());
      currentPackageNode = currentPackageNode.parent().getOrThrow();
    }
    return establishPackageHierarchyByName(targetRoot, packageStack);
  }

  private TreeNode<Symbol> establishPackageHierarchyByName(TreeNode<Symbol> targetRoot, Stack<Symbol> packageStack) {
    var currentParent = targetRoot;
    while (!packageStack.isEmpty()) {
      var packageSymbol = packageStack.pop();
      final TreeNode<Symbol> finalCurrentParent = currentParent;
      currentParent = ListsX.find(
          currentParent.children(),
          m -> m.value().getName().equals(packageSymbol.getName()) && m.value().getKind() == Kind.PACKAGE
        )
        .getOrCompute(() -> finalCurrentParent.addByValue(packageSymbol.clone()));
    }
    return currentParent;
  }

  private TreeNode<Symbol> establishPackageHierarchyByPath(TreeNode<Symbol> targetRoot, CtCompilationUnit spoonUnit) {
    var spoonPackage = spoonUnit.getPackageDeclaration().getReference().getDeclaration();
    var packageStack = new Stack<CtPackage>();
    var currentPackage = spoonPackage;
    while (!Spoon.isRootPackage(currentPackage)) {
      packageStack.push(currentPackage);
      currentPackage = currentPackage.getDeclaringPackage();
    }
    // Set root path in case it is unset
    targetRoot.value().putProperty(PathProperty.fromElement(currentPackage));
    return establishPackageHierarchyByPath(targetRoot, packageStack);
  }

  private TreeNode<Symbol> establishPackageHierarchyByPath(TreeNode<Symbol> targetRoot, Stack<CtPackage> packageStack) {
    var currentParent = targetRoot;
    while (!packageStack.isEmpty()) {
      var spoonPackage = packageStack.pop();
      final TreeNode<Symbol> finalCurrentParent = currentParent;
      currentParent = ListsX.find(
          currentParent.children(),
          m -> m.value().getPath().equals(CtEqPath.of(spoonPackage)) && m.value().getKind() == Kind.PACKAGE
        )
        .getOrCompute(() -> {
          var properties = PropertyMap.builder()
            .property(Kind.PACKAGE.toProperty())
            .property(SimpleNameProperty.fromElement(spoonPackage))
            .property(PathProperty.fromElement(spoonPackage))
            .build();
          var packageSymbol = Symbol.withPropertyMap(properties);
          return finalCurrentParent.addByValue(packageSymbol);
        });
    }
    return currentParent;
  }

  private TreeNode<Symbol> getTargetRoot(Array<SymbolWorkspace> parentWorkspaces) {
    var inheritedRoot = Options.getFirst(parentWorkspaces)
      .map(SymbolWorkspace::getTree)
      .map(TreeNode::value);
    var properties = PropertyMap.builder()
      .property(SimpleNameProperty.forRootPackage())
      .property(Kind.PACKAGE.toProperty())
      .property(inheritedRoot.map(s -> s.getProperty(PathProperty.class)).getOrElse(PathProperty.EMPTY))
      .build();
    return new TreeNode<>(Symbol.withPropertyMap(properties));
  }

  private record TargetEntry(TreeNode<Symbol> packageNode, TreeNode<Symbol> fileTree, CtCompilationUnit spoonUnit) {

  }

}
