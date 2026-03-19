import type { IFuseOptions } from 'fuse.js'

export function fuseOptions<T>(options: IFuseOptions<T>): IFuseOptions<T> {
  return {
    isCaseSensitive: false,
    ignoreDiacritics: true,
    includeScore: true,
    includeMatches: true,
    minMatchCharLength: 1,
    shouldSort: true,
    findAllMatches: false,
    // ignoreLocation: true,
    fieldNormWeight: 0.5,
    ...options,
  }
}
