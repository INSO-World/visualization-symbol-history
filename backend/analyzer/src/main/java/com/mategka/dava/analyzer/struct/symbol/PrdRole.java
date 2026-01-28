package com.mategka.dava.analyzer.struct.symbol;

public enum PrdRole {
  /// One-to-one succession (e.g., strand fork)
  DIRECT,
  /// Moved or renamed (e.g., for conflict avoidance)
  MOVED,
  /// Two symbols created after a LCA fork of both strands coincided
  MERGED,
  /// Symbol existed before LCA fork, had DIRECT or UPDATED successions on at least one parent ever since
  UPDATED,
}
