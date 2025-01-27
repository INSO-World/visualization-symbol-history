package com.mategka.dava.analyzer;

import com.mategka.dava.analyzer.extension.Lists;
import com.mategka.dava.analyzer.spoon.Spoon;
import com.mategka.dava.analyzer.struct.SymbolCreationContext;
import com.mategka.dava.analyzer.struct.property.*;
import com.mategka.dava.analyzer.struct.symbol.Symbol;

import lombok.Value;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Value
public class SymbolAdder {

  SymbolCreationContext context;

  private static Symbol.SymbolBuilder commonSymbolBuilder(SymbolCreationContext context, CtElement element) {
    var annotations = Lists.map(
      element.getAnnotations(),
      a -> TypeValue.UnknownType.of(a.getAnnotationType().getQualifiedName())
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

  private static List<CtElement> getVariables(CtBodyHolder element) {
    if (element.getBody() == null) {
      return Collections.emptyList();
    }
    return element.getBody().getDirectChildren().stream()
      .flatMap(childElement -> {
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
      })
      .toList();
  }

  private List<Symbol> parseSymbols(CtElement element, Symbol parent) {
    if (element instanceof CtType<?> type) {
      return parseTypeDeclaration(type, parent);
    } else if (element instanceof CtConstructor<?> constructor) {
      if (!Spoon.isDefaultConstructor(constructor) && !constructor.isCompactConstructor()) {
        return parseConstructor(constructor, parent);
      }
    } else if (element instanceof CtParameter<?> parameter) {
      return parseParameter(parameter, parent);
    } else if (element instanceof CtEnumValue<?> enumConstant) {
      return parseEnumConstant(enumConstant, parent);
    } else if (element instanceof CtField<?> field) {
      return parseField(field, parent);
    } else if (element instanceof CtMethod<?> method) {
      return parseMethod(method, parent);
    } else if (element instanceof CtLocalVariable<?> variable) {
      return parseVariable(variable, parent);
    }
    return Collections.emptyList();
  }

  List<Symbol> parseTypeDeclaration(CtType<?> typeDeclaration, Symbol parent) {
    var visibility = VisibilityProperty.Visibility.fromModifiable(typeDeclaration);
    var members = typeDeclaration.getTypeMembers();
    var symbol = commonSymbolBuilder(context, typeDeclaration)
      .property(KindProperty.fromType(typeDeclaration))
      .property(ParentProperty.fromSymbol(parent))
      .property(visibility.toProperty())
      .build();
    return Lists.cons(
      symbol,
      Lists.flatMap(members, m -> parseSymbols(m, symbol))
    );
  }

  private List<Symbol> parseConstructor(CtConstructor<?> constructor, Symbol parent) {
    var name = parent.getProperty(SimpleNameProperty.class).value();
    var visibility = VisibilityProperty.Visibility.fromModifiable(constructor);
    var variables = getVariables(constructor);
    var symbol = commonSymbolBuilder(context, constructor)
      .property(KindProperty.Value.CONSTRUCTOR.toProperty())
      .property(ParentProperty.fromSymbol(parent))
      .property(new SimpleNameProperty(name))
      .property(visibility.toProperty())
      .build();
    return Lists.cons(
      symbol,
      Lists.flatMap(constructor.getParameters(), p -> parseSymbols(p, symbol)),
      Lists.flatMap(variables, v -> parseSymbols(v, symbol))
    );
  }

  private List<Symbol> parseMethod(CtMethod<?> method, Symbol parent) {
    var visibility = VisibilityProperty.Visibility.fromModifiable(method);
    var variables = getVariables(method);
    var symbol = commonSymbolBuilder(context, method)
      .property(KindProperty.Value.METHOD.toProperty())
      .property(ParentProperty.fromSymbol(parent))
      .property(visibility.toProperty())
      .build();
    return Lists.cons(
      symbol,
      Lists.flatMap(method.getParameters(), p -> parseSymbols(p, symbol)),
      Lists.flatMap(variables, v -> parseSymbols(v, symbol))
    );
  }

  private List<Symbol> parseParameter(CtParameter<?> parameter, Symbol parent) {
    var symbol = commonSymbolBuilder(context, parameter)
      .property(KindProperty.Value.PARAMETER.toProperty())
      .property(ParentProperty.fromSymbol(parent))
      .build();
    return List.of(symbol);
  }

  private List<Symbol> parseEnumConstant(CtEnumValue<?> enumConstant, Symbol parent) {
    var arguments = Optional.ofNullable(enumConstant.getDefaultExpression())
      .map(i -> (CtConstructorCall<?>) i)
      .map(CtAbstractInvocation::getArguments)
      .orElseGet(Collections::emptyList);
    assert parent.getProperty(KindProperty.class).value() == KindProperty.Value.ENUM;
    var symbol = commonSymbolBuilder(context, enumConstant)
      .property(KindProperty.Value.ENUM_CONSTANT.toProperty())
      .property(ParentProperty.fromSymbol(parent))
      .build();
    return List.of(symbol);
  }

  private List<Symbol> parseField(CtField<?> field, Symbol parent) {
    var modifiers = ModifiersProperty.getModifiers(field);
    var kind = modifiers.containsAll(ModifiersProperty.Modifier.CONSTANT_FIELD_MODIFIERS)
      ? KindProperty.Value.CONSTANT
      : KindProperty.Value.FIELD;
    var initialValue = Optional.ofNullable(field.getDefaultExpression());
    var symbol = commonSymbolBuilder(context, field)
      .property(kind.toProperty())
      .property(ParentProperty.fromSymbol(parent))
      .build();
    return List.of(symbol);
  }

  private List<Symbol> parseVariable(CtLocalVariable<?> variable, Symbol parent) {
    var modifiers = ModifiersProperty.getModifiers(variable);
    var kind = modifiers.containsAll(ModifiersProperty.Modifier.CONSTANT_VARIABLE_MODIFIERS)
      ? KindProperty.Value.CONSTANT
      : KindProperty.Value.VARIABLE;
    var initialValue = Optional.ofNullable(variable.getDefaultExpression());
    var symbol = commonSymbolBuilder(context, variable)
      .property(kind.toProperty())
      .property(ParentProperty.fromSymbol(parent))
      .build();
    return List.of(symbol);
  }

}
