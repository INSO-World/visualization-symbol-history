package com.mategka.dava.analyzer.struct;

import com.mategka.dava.analyzer.spoon.Spoon;
import com.mategka.dava.analyzer.struct.property.*;
import com.mategka.dava.analyzer.struct.symbol.Symbol;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.*;
import lombok.experimental.FieldDefaults;
import spoon.reflect.CtModel;
import spoon.reflect.CtModelImpl;
import spoon.reflect.declaration.CtPackage;
import spoon.support.compiler.VirtualFile;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class StrandWorkspace {

  private static final String ROOT_PACKAGE_NAME = "ROOT";

  @NonNull
  @Getter
  final Strand strand;

  @Getter
  final Map<String, VirtualFile> files = new HashMap<>();

  final Map<String, Symbol> packages = new TreeMap<>();

  @Getter
  final Multimap<Symbol, Symbol> parentsToChildren = HashMultimap.create();

  CtModel model = Spoon.EMPTY_MODEL;

  public Symbol getPackage(CtPackage pakkage, SymbolCreationContext context) {
    var existingSymbol = packages.get(pakkage.getQualifiedName());
    if (existingSymbol != null) {
      return existingSymbol;
    }
    if (pakkage instanceof CtModelImpl.CtRootPackage) {
      var rootPackage = context.symbolBuilder()
        .property(new SimpleNameProperty(ROOT_PACKAGE_NAME))
        .property(KindProperty.Value.PACKAGE.toProperty())
        .property(new PathProperty(Spoon.EMPTY_PATH))
        .build();
      packages.put(pakkage.getQualifiedName(), rootPackage);
      return rootPackage;
    }
    var parentPackage = pakkage.getDeclaringPackage();
    var parentSymbol = getPackage(parentPackage, context);
    var childName = pakkage.getSimpleName();
    var childSymbol = context.symbolBuilder()
      .property(KindProperty.Value.PACKAGE.toProperty())
      .property(new SimpleNameProperty(childName))
      .property(new ParentProperty(parentSymbol.getId()))
      .property(new PathProperty(pakkage.getPath()))
      .build();
    packages.put(pakkage.getQualifiedName(), childSymbol);
    parentsToChildren.put(parentSymbol, childSymbol);
    return childSymbol;
  }

  private void updateModel(CtModel model) {
    this.model = model;
  }

}
