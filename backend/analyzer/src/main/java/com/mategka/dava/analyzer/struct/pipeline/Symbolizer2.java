package com.mategka.dava.analyzer.struct.pipeline;

import com.mategka.dava.analyzer.extension.TreeNode;
import com.mategka.dava.analyzer.extension.option.Options;
import com.mategka.dava.analyzer.struct.property.KindProperty;
import com.mategka.dava.analyzer.struct.property.ParentProperty;
import com.mategka.dava.analyzer.struct.property.value.Kind;
import com.mategka.dava.analyzer.struct.symbol.Symbol2;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class Symbolizer2 {

  @Contract(mutates = "param")
  public @NotNull TreeNode<Symbol2> augmentParentProperty(@NotNull TreeNode<Symbol2> root) {
    //noinspection CodeBlock2Expr
    root.iterator().forEachRemaining(node -> {
      node.parent().ifSome(p -> node.value().putProperty(ParentProperty.fromSymbol(p.value())));
    });
    return root;
  }

  public TreeNode<Symbol2> symbolize(@NotNull CtElement rootElement, @NotNull Symbol2 baseParent) {
    var parentElement = Options.when(rootElement.isParentInitialized(), rootElement::getParent).getOrElse(rootElement);
    final TreeNode<Symbol2> virtualRoot = new TreeNode<>(baseParent);
    final Map<CtElement, TreeNode<Symbol2>> symbolMap = new HashMap<>();
    ElementCapture.parseElement(rootElement, parentElement)
      .forEachOrdered(s -> {
        var parent = symbolMap.computeIfAbsent(s.getParent(), _k -> virtualRoot);
        var symbol = PropertyCapture2.parseElement(s.getElement());
        var symbolNode = new TreeNode<>(symbol);
        symbolMap.put(s.getElement(), symbolNode);
        parent.add(symbolNode);
      });
    return virtualRoot.children().getFirst().toRoot();
  }

  public TreeNode<Symbol2> symbolizeFileType(@NotNull CtType<?> typeDeclaration, @NotNull Symbol2 packageSymbol) {
    if (packageSymbol.getPropertyValue(KindProperty.class).getOrThrow() != Kind.PACKAGE) {
      throw new IllegalArgumentException("Given parent symbol was not a package-kind symbol");
    }
    return symbolize(typeDeclaration, packageSymbol);
  }

}
