package com.mategka.dava.analyzer.collections;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import lombok.experimental.Delegate;

@SuppressWarnings("UnstableApiUsage")
public class DAG<N> implements MutableGraph<N> {

  @Delegate
  private final MutableGraph<N> graph = GraphBuilder.directed().allowsSelfLoops(false).build();

  public int nodeCount() {
    return graph.nodes().size();
  }

}
