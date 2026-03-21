export function capitalize(s: string): string {
  return s.charAt(0).toUpperCase() + s.slice(1)
}

export function union<T>(sets: Array<Set<T>>): Set<T> {
  const result = new Set<T>()
  for (const set of sets) {
    set.forEach((v) => result.add(v))
  }
  return result
}

export function setOf<T>(...arrays: Array<Array<T>>): T[] {
  return [...new Set<T>(arrays.flat(1))]
}
