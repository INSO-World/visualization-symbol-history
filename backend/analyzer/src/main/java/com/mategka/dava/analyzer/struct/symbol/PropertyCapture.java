package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.extension.ListsX;
import com.mategka.dava.analyzer.extension.option.Option;
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

import lombok.experimental.UtilityClass;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtWildcardReference;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

@UtilityClass
public class PropertyCapture {

  public BareSymbol parseElement(CtElement element) {
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

  private PropertyMap.Builder commonPropertiesBuilder(CtElement element) {
    var annotations = ListsX.map(
      element.getAnnotations(),
      a -> UnknownType.of(a.getAnnotationType().getQualifiedName())
    );
    var builder = PropertyMap.builder()
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
      // TODO: Replace with known type where applicable (may be generic type parameter)
      builder.property(new TypeProperty(parseUnknownType(typedElement.getType())));
    }
    if (element instanceof CtFormalTypeDeclarer formalTypeDeclarer) {
      var typeParameters = ListsX.map(
        formalTypeDeclarer.getFormalCtTypeParameters(), PropertyCapture::parseTypeParameter);
      builder.property(new TypeParametersProperty(typeParameters));
    }
    return builder;
  }

  private BareSymbol parseConstructor(CtConstructor<?> constructor) {
    if (!Spoon.isRegularConstructor(constructor)) {
      throw new IllegalArgumentException("Given subject constitutes an illegal constructor symbol basis");
    }
    var name = Option.cast(constructor.getParent(), CtNamedElement.class)
      .map(CtNamedElement::getSimpleName)
      .getOrElse("");
    var visibility = Visibility.fromModifiable(constructor);
    var properties = commonPropertiesBuilder(constructor)
      .property(Kind.CONSTRUCTOR.toProperty())
      .property(new SimpleNameProperty(name))
      .property(visibility.toProperty())
      .build();
    return new BareSymbol(properties);
  }

  private BareSymbol parseEnumConstant(CtEnumValue<?> enumConstant) {
    var arguments = Option.fromNullable(enumConstant.getDefaultExpression())
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
    return new BareSymbol(properties);
  }

  private BareSymbol parseField(CtField<?> field) {
    var modifiers = ModifiersProperty.getModifiers(field);
    var kind = modifiers.containsAll(Modifier.CONSTANT_FIELD_MODIFIERS)
      ? Kind.CONSTANT_FIELD
      : Kind.FIELD;
    var initialValue = parseNullableExpression(field.getDefaultExpression()).map(InitialValueProperty::new);
    var properties = commonPropertiesBuilder(field)
      .property(kind.toProperty())
      .property(initialValue)
      .build();
    return new BareSymbol(properties);
  }

  private BareSymbol parseMethod(CtMethod<?> method) {
    var visibility = Visibility.fromModifiable(method);
    var properties = commonPropertiesBuilder(method)
      .property(Kind.METHOD.toProperty())
      .property(visibility.toProperty())
      .build();
    return new BareSymbol(properties);
  }

  private Option<Expression> parseNullableExpression(CtExpression<?> expression) {
    return Option.fromNullable(expression).map(Expression::fromSpoon);
  }

  private BareSymbol parseParameter(CtParameter<?> parameter) {
    var properties = commonPropertiesBuilder(parameter)
      .property(Kind.PARAMETER.toProperty())
      .build();
    return new BareSymbol(properties);
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

  private BareSymbol parseTypeDeclaration(CtType<?> typeDeclaration) {
    var visibility = Visibility.fromModifiable(typeDeclaration);
    var ctSupertypes = typeDeclaration.isInterface()
      ? typeDeclaration.getSuperInterfaces()
      : Set.of(typeDeclaration.getSuperclass());
    var supertypes = ListsX.map(ctSupertypes, PropertyCapture::parseUnknownType);
    Set<CtTypeReference<?>> ctRealizations = typeDeclaration.isInterface()
      ? Collections.emptySet()
      : typeDeclaration.getSuperInterfaces();
    var realizations = ListsX.map(ctRealizations, PropertyCapture::parseUnknownType);
    var properties = commonPropertiesBuilder(typeDeclaration)
      .property(KindProperty.fromType(typeDeclaration))
      .property(visibility.toProperty())
      .property(new SupertypesProperty(supertypes))
      .property(new RealizationsProperty(realizations))
      .build();
    return new BareSymbol(properties);
  }

  private TypeParameter parseTypeParameter(CtTypeParameter typeParameter) {
    var name = typeParameter.getSimpleName();
    var bound = Option.fromNullable(typeParameter.getSuperclass())
      .map(Object::toString)
      .map(UnknownType::of)
      .map(UpperTypeBound::new);
    return bound.fold(b -> TypeParameter.of(name, b), () -> TypeParameter.of(name));
  }

  private Type parseUnknownType(CtTypeReference<?> typeReference) {
    var typeArguments = ListsX.map(typeReference.getActualTypeArguments(), PropertyCapture::parseTypeArgument);
    return UnknownType.of(typeReference.getQualifiedName(), typeArguments);
  }

  private BareSymbol parseVariable(CtLocalVariable<?> variable) {
    var modifiers = ModifiersProperty.getModifiers(variable);
    var kind = modifiers.containsAll(Modifier.CONSTANT_VARIABLE_MODIFIERS)
      ? Kind.CONSTANT_VARIABLE
      : Kind.VARIABLE;
    var initialValue = parseNullableExpression(variable.getDefaultExpression()).map(InitialValueProperty::new);
    var properties = commonPropertiesBuilder(variable)
      .property(kind.toProperty())
      .property(initialValue)
      .build();
    return new BareSymbol(properties);
  }

}
