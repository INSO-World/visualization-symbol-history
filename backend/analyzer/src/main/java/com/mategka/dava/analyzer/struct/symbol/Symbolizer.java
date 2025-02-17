package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.extension.ListsX;
import com.mategka.dava.analyzer.extension.Streamer;
import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.spoon.Spoon;
import com.mategka.dava.analyzer.struct.property.*;
import com.mategka.dava.analyzer.struct.property.value.*;

import lombok.Value;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.declaration.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Value
public class Symbolizer {

  SymbolCreationContext context;

  private static Symbol.SymbolBuilder commonSymbolBuilder(SymbolCreationContext context, CtElement element) {
    var annotations = ListsX.map(
      element.getAnnotations(),
      a -> UnknownType.of(a.getAnnotationType().getQualifiedName())
    );
    var builder = context.symbolBuilder()
      .property(new AnnotationsProperty(annotations))
      .property(LineRangeProperty.fromElement(element))
      .property(PathProperty.fromElement(element));
    if (element instanceof CtNamedElement namedElement) {
      builder.property(SimpleNameProperty.fromElement(namedElement));
    }
    if (element instanceof CtModifiable modifiable) {
      builder.property(ModifiersProperty.fromModifiable(modifiable));
    }
    if (element instanceof CtTypedElement<?> typedElement) {
      // TODO: Replace with known type where applicable (may be generic type parameter!)
      builder.property(TypeProperty.unknownFromTypedElement(typedElement));
    }
    // TODO: Add type parameters (declarations) if applicable
    return builder;
  }

  public Streamer<Symbol> symbolizeType(CtType<?> typeDeclaration, Symbol packageSymbol) {
    if (packageSymbol.getPropertyValue(KindProperty.class).getOrThrow() != Kind.PACKAGE) {
      throw new IllegalArgumentException("Given parent symbol was not a package-kind symbol");
    }
    return symbolize(typeDeclaration, packageSymbol);
  }

  public Streamer<Symbol> symbolize(CtElement rootElement, Symbol baseParent) {
    var parentElement = Option.when(rootElement.isParentInitialized(), rootElement::getParent).getOrElse(rootElement);
    return ElementCapture.parseElement(rootElement, parentElement)
      .map(mapper(baseParent));
  }

  public Function<Subject, Symbol> mapper(Symbol baseParent) {
    final Map<CtElement, Symbol> symbolMap = new HashMap<>();
    return s -> {
      var parent = symbolMap.computeIfAbsent(s.getParent(), _k -> baseParent);
      var symbol = parseElement(s.getElement())
        .withProperty(ParentProperty.fromSymbol(parent));
      symbolMap.put(s.getElement(), symbol);
      return symbol;
    };
  }

  private Symbol parseElement(CtElement element) {
    return switch (element) {
      case CtType<?> type -> parseTypeDeclaration(type);
      case CtConstructor<?> constructor -> parseConstructor(constructor);
      case CtParameter<?> parameter -> parseParameter(parameter);
      case CtEnumValue<?> enumConstant -> parseEnumConstant(enumConstant);
      case CtField<?> field -> parseField(field);
      case CtMethod<?> method -> parseMethod(method);
      case CtLocalVariable<?> variable -> parseVariable(variable);
      default -> throw new IllegalArgumentException("Given subject constitutes an illegal symbol basis");
    };
  }

  private Symbol parseTypeDeclaration(CtType<?> typeDeclaration) {
    var visibility = Visibility.fromModifiable(typeDeclaration);
    return commonSymbolBuilder(context, typeDeclaration)
      .property(KindProperty.fromType(typeDeclaration))
      .property(visibility.toProperty())
      .build();
  }

  private Symbol parseConstructor(CtConstructor<?> constructor) {
    if (!Spoon.isRegularConstructor(constructor)) {
      throw new IllegalArgumentException("Given subject constitutes an illegal constructor symbol basis");
    }
    var name = Option.cast(constructor.getParent(), CtNamedElement.class)
      .map(CtNamedElement::getSimpleName)
      .getOrElse("");
    var visibility = Visibility.fromModifiable(constructor);
    return commonSymbolBuilder(context, constructor)
      .property(Kind.CONSTRUCTOR.toProperty())
      .property(new SimpleNameProperty(name))
      .property(visibility.toProperty())
      .build();
  }

  private Symbol parseMethod(CtMethod<?> method) {
    var visibility = Visibility.fromModifiable(method);
    return commonSymbolBuilder(context, method)
      .property(Kind.METHOD.toProperty())
      .property(visibility.toProperty())
      .build();
  }

  private Symbol parseParameter(CtParameter<?> parameter) {
    return commonSymbolBuilder(context, parameter)
      .property(Kind.PARAMETER.toProperty())
      .build();
  }

  private Symbol parseEnumConstant(CtEnumValue<?> enumConstant) {
    var arguments = Option.fromNullable(enumConstant.getDefaultExpression())
      .map(i -> (CtConstructorCall<?>) i)
      .map(CtAbstractInvocation::getArguments)
      .getOrCompute(Collections::emptyList);
    return commonSymbolBuilder(context, enumConstant)
      .property(Kind.ENUM_CONSTANT.toProperty())
      .build();
  }

  private Symbol parseField(CtField<?> field) {
    var modifiers = ModifiersProperty.getModifiers(field);
    var kind = modifiers.containsAll(Modifier.CONSTANT_FIELD_MODIFIERS)
      ? Kind.CONSTANT
      : Kind.FIELD;
    var initialValue = Option.fromNullable(field.getDefaultExpression());
    return commonSymbolBuilder(context, field)
      .property(kind.toProperty())
      .build();
  }

  private Symbol parseVariable(CtLocalVariable<?> variable) {
    var modifiers = ModifiersProperty.getModifiers(variable);
    var kind = modifiers.containsAll(Modifier.CONSTANT_VARIABLE_MODIFIERS)
      ? Kind.CONSTANT
      : Kind.VARIABLE;
    var initialValue = Option.fromNullable(variable.getDefaultExpression());
    return commonSymbolBuilder(context, variable)
      .property(kind.toProperty())
      .build();
  }

}
