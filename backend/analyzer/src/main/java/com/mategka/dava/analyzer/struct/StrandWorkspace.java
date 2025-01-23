package com.mategka.dava.analyzer.struct;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mategka.dava.analyzer.struct.property.*;
import com.mategka.dava.analyzer.struct.symbol.Symbol;
import com.mategka.dava.analyzer.util.Spoon;
import lombok.*;
import lombok.experimental.FieldDefaults;
import spoon.reflect.CtModel;
import spoon.support.compiler.VirtualFile;

import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class StrandWorkspace {

  @NonNull
  @Getter
  final Long strandId;

  @Getter
  final Map<String, VirtualFile> files = new HashMap<>();

  final Map<String, Symbol> packages = new TreeMap<>();

  @Getter
  final Multimap<Symbol, Symbol> parentsToChildren = HashMultimap.create();

  CtModel model = Spoon.EMPTY_MODEL;

  public Symbol getPackage(String name, SymbolCreationContext context) {
    if (!packages.containsKey(name)) {
      var parts = Arrays.asList(name.split("\\."));
      for (int i = 1; i <= parts.size(); i++) {
        var currentParts = parts.subList(0, i);
        var currentPath = String.join(".", currentParts);
        packages.computeIfAbsent(currentPath, _p -> newPackage(currentParts, context));
      }
    }
    return packages.get(name);
  }

  private Symbol newPackage(List<String> nameParts, SymbolCreationContext context) {
    var parentName = String.join(".", nameParts.subList(0, nameParts.size() - 1));
    var parentPackage = Optional.ofNullable(packages.get(parentName));
    var parentPackageId = parentPackage.map(Symbol::getId).orElse(null);
    var name = nameParts.getLast();
    var symbol = context.symbolBuilder()
      .property(KindProperty.Value.PACKAGE.toProperty())
      .property(new ParentProperty(parentPackageId))
      .property(new SimpleNameProperty(name))
      .property(new PathProperty(null)) // TODO: Correct path
      .build();
    parentPackage.ifPresent(value -> parentsToChildren.put(value, symbol));
    return symbol;
  }

  private void updateModel(CtModel model) {
    this.model = model;
  }

}
