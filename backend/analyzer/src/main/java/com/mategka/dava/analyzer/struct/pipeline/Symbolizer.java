package com.mategka.dava.analyzer.struct.pipeline;

import com.mategka.dava.analyzer.extension.option.Options;
import com.mategka.dava.analyzer.extension.struct.TreeNode;
import com.mategka.dava.analyzer.struct.property.KindProperty;
import com.mategka.dava.analyzer.struct.property.value.Kind;
import com.mategka.dava.analyzer.struct.symbol.Symbol;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class Symbolizer {

  public TreeNode<Symbol> symbolize(@NotNull CtElement rootElement, @NotNull Symbol baseParent) {
    var parentElement = Options.when(rootElement.isParentInitialized(), rootElement::getParent).getOrElse(rootElement);
    final TreeNode<Symbol> virtualRoot = new TreeNode<>(baseParent);
    final Map<CtElement, TreeNode<Symbol>> symbolMap = new HashMap<>();
    ElementCapture.parseElement(rootElement, parentElement)
      .forEachOrdered(s -> {
        var parent = symbolMap.computeIfAbsent(s.getParent(), _k -> virtualRoot);
        var symbol = PropertyCapture.parseElement(s.getElement());
        var symbolNode = new TreeNode<>(symbol);
        symbolMap.put(s.getElement(), symbolNode);
        parent.add(symbolNode);
      });
    PropertyCapture.clearPathCache();
    return virtualRoot.children().getFirst().toRoot();
  }

  public TreeNode<Symbol> symbolizeFileType(@NotNull CtType<?> typeDeclaration, @NotNull Symbol packageSymbol) {
    if (packageSymbol.getPropertyValue(KindProperty.class).getOrThrow() != Kind.PACKAGE) {
      throw new IllegalArgumentException("Given parent symbol was not a package-kind symbol");
    }
    return symbolize(typeDeclaration, packageSymbol);
  }

}
