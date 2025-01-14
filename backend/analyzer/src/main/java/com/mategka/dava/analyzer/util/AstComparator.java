package com.mategka.dava.analyzer.util;

import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.ConfigurationOptions;
import com.github.gumtreediff.matchers.GumtreeProperties;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.DiffConfiguration;
import spoon.reflect.declaration.CtElement;

public final class AstComparator {

  private final gumtree.spoon.AstComparator comparator = new gumtree.spoon.AstComparator();
  private final DiffConfiguration diffConfiguration;

  public AstComparator() {
    DiffConfiguration diffConfiguration = new DiffConfiguration();
    diffConfiguration.setMatcher(new CompositeMatchers.ClassicGumtree());
    GumtreeProperties gumtreeProperties = new GumtreeProperties();
    gumtreeProperties.tryConfigure(ConfigurationOptions.bu_minsim, 0.2);
    gumtreeProperties.tryConfigure(ConfigurationOptions.bu_minsize, 600);
    gumtreeProperties.tryConfigure(ConfigurationOptions.st_minprio, 1);
    gumtreeProperties.tryConfigure(ConfigurationOptions.st_priocalc, "size");
    diffConfiguration.setGumtreeProperties(gumtreeProperties);
    this.diffConfiguration = diffConfiguration;
  }

  public Diff compare(CtElement left, CtElement right) {
    return comparator.compare(left, right, diffConfiguration);
  }

}
