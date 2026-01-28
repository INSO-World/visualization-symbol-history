package com.mategka.dava.analyzer.struct.symbol;

public enum PrdRole {
  /// One-to-one succession (e.g., strand fork) or same-identity merge parent in a many-to-one succession
  DIRECT,
  /// Moved or renamed (e.g., for conflict avoidance)
  MOVED,
  /// Different-identity merge parent in a many-to-one succession
  MERGED,
  /// Symbol existed before LCA fork, had DIRECT or UPDATED successions on at least one parent ever since
  UPDATED,
}
