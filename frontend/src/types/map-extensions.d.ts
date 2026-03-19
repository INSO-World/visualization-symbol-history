interface MapConstructor {
  groupBy<K, T>(items: Iterable<T>, keyFn: (item: T) => K): Map<K, T[]>
}
