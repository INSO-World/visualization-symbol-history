package com.mategka.dava.analyzer.struct.pipeline;

import com.mategka.dava.analyzer.extension.ListsX;
import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.option.Options;
import com.mategka.dava.analyzer.spoon.Spoon;
import com.mategka.dava.analyzer.struct.property.*;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;
import com.mategka.dava.analyzer.struct.property.value.*;
import com.mategka.dava.analyzer.struct.property.value.argument.ConcreteTypeArgument;
import com.mategka.dava.analyzer.struct.property.value.argument.TypeArgument;
import com.mategka.dava.analyzer.struct.property.value.argument.WildcardTypeArgument;
import com.mategka.dava.analyzer.struct.property.value.bound.TypeBound;
import com.mategka.dava.analyzer.struct.property.value.bound.UpperTypeBound;
import com.mategka.dava.analyzer.struct.property.value.type.Type;
import com.mategka.dava.analyzer.struct.property.value.type.UnknownType;
import com.mategka.dava.analyzer.struct.symbol.Symbol;

import lombok.experimental.UtilityClass;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtWildcardReference;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

@UtilityClass
public class PropertyCapture {

  public Symbol parseElement(CtElement element) {
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

  public Symbol parsePackage(CtPackage pakkage) {
    var properties = commonPropertiesBuilder(pakkage)
      .property(Kind.PACKAGE.toProperty())
      .build();
    return Symbol.withPropertyMap(properties);
  }

  private PropertyMap.Builder commonPropertiesBuilder(CtElement element) {
    var annotations = ListsX.map(
      element.getAnnotations(),
      a -> (Type) UnknownType.of(a.getAnnotationType().getQualifiedName())
    );
    var ctPathProperty = CtPathProperty.fromElement(element);
    var builder = PropertyMap.builder()
      //.property(AnalyzerLevelProperty.CURRENT)
      .property(AnnotationsProperty::new, annotations)
      .property(ctPathProperty)
      .property(PathProperty.fromCtPathProperty(ctPathProperty));
    if (element.getPosition().isValidPosition()) {
      builder.property(LineRangeProperty.fromElement(element));
    }
    if (element instanceof CtNamedElement namedElement) {
      builder.property(SimpleNameProperty.fromElement(namedElement));
    }
    if (element instanceof CtModifiable modifiable) {
      builder.property(ModifiersProperty.fromModifiable(modifiable));
    }
    if (element instanceof CtTypedElement<?> typedElement) {
      // TODO: Replace with known type where applicable (may be generic type parameter)
      builder.property(new TypeProperty(parseUnknownType(typedElement.getType())));
    }
    if (element instanceof CtFormalTypeDeclarer formalTypeDeclarer && !formalTypeDeclarer.getFormalCtTypeParameters()
      .isEmpty()) {
      var typeParameters = ListsX.map(
        formalTypeDeclarer.getFormalCtTypeParameters(), PropertyCapture::parseTypeParameter);
      builder.property(new TypeParametersProperty(typeParameters));
    }
    if (element instanceof CtExecutable<?> executable) {
      // TODO: FIX: Does not seem to hold equality checks (i.e., equal bodies don't always result in equal hashes)
      String bodyString;
      try {
        bodyString = Objects.toString(executable.getBody());
      } catch (Exception _ignored) {
        // If the function body can - for some reason - not be parsed, treat it as empty
        bodyString = "";
      }
      builder.property(BodyHashProperty.fromString(bodyString));
    }
    return builder;
  }

  private Symbol parseConstructor(CtConstructor<?> constructor) {
    if (!Spoon.isRegularConstructor(constructor)) {
      throw new IllegalArgumentException("Given subject constitutes an illegal constructor symbol basis");
    }
    var name = Options.cast(constructor.getParent(), CtNamedElement.class)
      .map(CtNamedElement::getSimpleName)
      .getOrElse("");
    var visibility = Visibility.fromModifiable(constructor);
    var properties = commonPropertiesBuilder(constructor)
      .property(Kind.CONSTRUCTOR.toProperty())
      .property(new SimpleNameProperty(name))
      .property(visibility.toProperty())
      .build();
    return Symbol.withPropertyMap(properties);
  }

  private Symbol parseEnumConstant(CtEnumValue<?> enumConstant) {
    var arguments = Options.fromNullable(enumConstant.getDefaultExpression())
      .map(i -> (CtConstructorCall<?>) i)
      .map(CtAbstractInvocation::getArguments)
      .getOrElse(Collections.emptyList())
      .stream()
      .map(Expression::fromSpoon)
      .toList();
    var properties = commonPropertiesBuilder(enumConstant)
      .property(Kind.ENUM_CONSTANT.toProperty())
      .property(new EnumArgumentsProperty(arguments))
      .build();
    return Symbol.withPropertyMap(properties);
  }

  private Symbol parseField(CtField<?> field) {
    var modifiers = ModifiersProperty.getModifiers(field);
    var kind = modifiers.containsAll(Modifier.CONSTANT_FIELD_MODIFIERS)
      ? Kind.CONSTANT_FIELD
      : Kind.FIELD;
    var initialValue = parseNullableExpression(field.getDefaultExpression()).map(InitialValueProperty::new);
    var properties = commonPropertiesBuilder(field)
      .property(kind.toProperty())
      .property(initialValue)
      .build();
    return Symbol.withPropertyMap(properties);
  }

  private Symbol parseMethod(CtMethod<?> method) {
    var visibility = Visibility.fromModifiable(method);
    var properties = commonPropertiesBuilder(method)
      .property(Kind.METHOD.toProperty())
      .property(visibility.toProperty())
      .build();
    return Symbol.withPropertyMap(properties);
  }

  private Option<Expression> parseNullableExpression(CtExpression<?> expression) {
    return Options.fromNullable(expression).map(Expression::fromSpoon);
  }

  private Symbol parseParameter(CtParameter<?> parameter) {
    var properties = commonPropertiesBuilder(parameter)
      .property(Kind.PARAMETER.toProperty())
      .build();
    return Symbol.withPropertyMap(properties);
  }

  private TypeArgument parseTypeArgument(CtTypeReference<?> typeReference) {
    if (typeReference instanceof CtWildcardReference wildcardReference) {
      Function<Type, TypeBound> constructor = wildcardReference.isUpper() ? TypeBound::upper : TypeBound::lower;
      var type = parseUnknownType(wildcardReference.getBoundingType());
      return new WildcardTypeArgument(constructor.apply(type));
    } else {
      return new ConcreteTypeArgument(parseUnknownType(typeReference));
    }
  }

  private Symbol parseTypeDeclaration(CtType<?> typeDeclaration) {
    var visibility = Visibility.fromModifiable(typeDeclaration);
    var ctSupertypes = typeDeclaration.isInterface()
      ? typeDeclaration.getSuperInterfaces()
      : Options.fromNullable(typeDeclaration.getSuperclass())
        .map(Set::of)
        .getOrCompute(Collections::emptySet);
    var supertypes = ListsX.map(ctSupertypes, PropertyCapture::parseUnknownType);
    Set<CtTypeReference<?>> ctRealizations = typeDeclaration.isInterface()
      ? Collections.emptySet()
      : typeDeclaration.getSuperInterfaces();
    var realizations = ListsX.map(ctRealizations, PropertyCapture::parseUnknownType);
    var properties = commonPropertiesBuilder(typeDeclaration)
      .property(KindProperty.fromType(typeDeclaration))
      .property(visibility.toProperty())
      .property(SupertypesProperty::new, supertypes)
      .property(RealizationsProperty::new, realizations)
      .build();
    return Symbol.withPropertyMap(properties);
  }

  private TypeParameter parseTypeParameter(CtTypeParameter typeParameter) {
    var name = typeParameter.getSimpleName();
    var bound = Options.fromNullable(typeParameter.getSuperclass())
      .map(Object::toString)
      .map(UnknownType::of)
      .map(UpperTypeBound::new);
    return bound.fold(b -> TypeParameter.of(name, b), () -> TypeParameter.of(name));
  }

  private Type parseUnknownType(CtTypeReference<?> typeReference) {
    var typeArguments = ListsX.map(typeReference.getActualTypeArguments(), PropertyCapture::parseTypeArgument);
    return UnknownType.of(typeReference.getQualifiedName(), typeArguments);
  }

  private Symbol parseVariable(CtLocalVariable<?> variable) {
    var modifiers = ModifiersProperty.getModifiers(variable);
    var kind = modifiers.containsAll(Modifier.CONSTANT_VARIABLE_MODIFIERS)
      ? Kind.CONSTANT_VARIABLE
      : Kind.VARIABLE;
    var initialValue = parseNullableExpression(variable.getDefaultExpression()).map(InitialValueProperty::new);
    var properties = commonPropertiesBuilder(variable)
      .property(kind.toProperty())
      .property(initialValue)
      .build();
    return Symbol.withPropertyMap(properties);
  }

}
