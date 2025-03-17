package com.mategka.dava.analyzer.struct.pipeline;

import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.struct.property.KindProperty;
import com.mategka.dava.analyzer.struct.property.ParentProperty;
import com.mategka.dava.analyzer.struct.property.value.Kind;
import com.mategka.dava.analyzer.struct.symbol.Subject;
import com.mategka.dava.analyzer.struct.symbol.Symbol;
import com.mategka.dava.analyzer.struct.symbol.SymbolCreationContext;

import lombok.Value;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Value
public class Symbolizer {

  SymbolCreationContext context;

  public AnStream<Symbol> symbolize(CtElement rootElement, Symbol baseParent) {
    var parentElement = Option.when(rootElement.isParentInitialized(), rootElement::getParent).getOrElse(rootElement);
    return ElementCapture.parseElement(rootElement, parentElement)
      .map(mapper(baseParent));
  }

  public AnStream<Symbol> symbolizeRootType(CtType<?> typeDeclaration, Symbol packageSymbol) {
    if (packageSymbol.getPropertyValue(KindProperty.class).getOrThrow() != Kind.PACKAGE) {
      throw new IllegalArgumentException("Given parent symbol was not a package-kind symbol");
    }
    return symbolize(typeDeclaration, packageSymbol);
  }

  private Function<Subject, Symbol> mapper(Symbol baseParent) {
    final Map<CtElement, Symbol> symbolMap = new HashMap<>();
    return s -> {
      var parent = symbolMap.computeIfAbsent(s.getParent(), _k -> baseParent);
      var symbol = PropertyCapture.parseElement(s.getElement())
        .withProperty(ParentProperty.fromSymbol(parent))
        .complete(context);
      symbolMap.put(s.getElement(), symbol);
      return symbol;
    };
  }

}
