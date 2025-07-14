import { defineStore } from 'pinia'
import {
  ChangeCause,
  type CommitDto,
  type KeyDto,
  type RootDto,
  type StateDto,
  type SymbolDto,
} from '@/models/analyzer'
import Fuse from 'fuse.js'
import { fuseOptions } from '@/constants/fuse-options'
import type { Range } from '@/models/common'
import { normalizeDate } from '@/functions/date'

type KeyRecord = {
  id: number
  keys: KeyDto[]
}

type FuseRecord = {
  name: string
} & KeyRecord

export type SearchResult = {
  symbol: SymbolDto
  key: KeyDto
  refIndex: number
  score: number
  match: Range[]
}

export type AnalyzerStore = ReturnType<typeof useAnalyzerStore>

export const useAnalyzerStore = defineStore('analyzer', {
  state: () => ({
    root: null as unknown as RootDto,
    loading: false,
    error: null as string | null,
    globalFuse: null as unknown as Fuse<FuseRecord>,
  }),
  actions: {
    async init() {
      this.loading = true
      this.error = null
      try {
        this.root = await fetch('result.json').then((res) => {
          if (!res.ok) {
            throw new Error(`Failed to fetch analyzer data: Status ${res.status}`)
          }
          return res.json() as Promise<RootDto>
        })
        const rawKeysToIds = new Map<string, Map<number, KeyRecord>>()
        for (const symbol of this.root.symbols) {
          for (const key of symbol.keys) {
            const map = rawKeysToIds.get(key.name) ?? new Map<number, KeyRecord>()
            if (map.size === 0) {
              rawKeysToIds.set(key.name, map)
            }
            const rec = map.get(symbol.id) ?? { id: symbol.id, keys: [] }
            if (rec.keys.length === 0) {
              map.set(symbol.id, rec)
            }
            rec.keys.push(key)
          }
        }
        console.log(rawKeysToIds)
        const fuseObject = [...rawKeysToIds.entries()].flatMap((e) =>
          [...e[1].values()].map(
            (r) =>
              ({
                name: e[0],
                ...r,
              }) satisfies FuseRecord,
          ),
        )
        console.log(fuseObject)
        this.globalFuse = new Fuse(
          fuseObject,
          fuseOptions({
            keys: ['name'],
          }),
        )
      } catch (error) {
        if (!(error instanceof Error)) {
          throw error
        }
        this.error = error.message
        console.error('Error initializing analyzer store', error)
      } finally {
        this.loading = false
      }
    },
    commitDate(index: number): Date {
      return new Date(this.root.commits[index].date)
    },
    getCommit(commitId: number): CommitDto {
      return this.root.commits[commitId]
    },
    search(query: string): SearchResult[] {
      const results = this.globalFuse.search(query).flatMap((result) => {
        const record: KeyRecord = result.item
        const symbol = this.root.symbols[record.id]
        if (symbol.id === 0 || symbol.keys.every((k) => k.name === 'serialVersionUID')) {
          return []
        }
        let score = Math.max(1 - Math.max(result.score! - 2e-6, 0) / 2, 0)
        if (symbol.deleted) {
          score -= 0.0001
        }
        return [
          {
            symbol,
            key: [...new Set(record.keys)][0]!,
            score,
            match: result.matches![0].indices.map(([from, to]) => [from, to + 1]),
            refIndex: result.refIndex,
          } satisfies SearchResult,
        ]
      })
      results.sort((a, b) => b.score - a.score)
      const ids = new Set<number>()
      return results.filter((result) => {
        if (ids.has(result.symbol.id)) {
          return false
        }
        ids.add(result.symbol.id)
        return true
      })
    },
    findKeyState(symbol: SymbolDto, key: KeyDto): StateDto {
      const keyIndex = symbol.keys.findIndex(
        (k) =>
          k.parent === key.parent &&
          k.from === key.from &&
          k.to === key.to &&
          k.name === key.name &&
          k.kind === key.kind,
      )
      if (keyIndex !== -1) {
        let offset = keyIndex
        const yearMonths = Object.keys(symbol.states)
        yearMonths.sort()
        for (const ym of yearMonths) {
          const states = symbol.states[ym]
          if (offset < states.length) {
            return states[offset]
          }
          offset -= states.length
        }
      }
      const keyTimestamp = new Date(key.from).valueOf()
      let weakTimestamp: number | null = null
      let strongCandidate: StateDto | null = null
      let weakCandidate: StateDto | null = null
      let weakestCandidate: StateDto | null = null
      for (const states of Object.values(symbol.states)) {
        for (const state of states) {
          const p = state.properties
          if (p['kind'] === key.kind && (!key.name || p['simpleName'] === key.name)) {
            const commitTimestamp = this.commitDate(state.commit).valueOf()
            if (commitTimestamp === keyTimestamp) {
              strongCandidate = state
            } else if (
              commitTimestamp < keyTimestamp &&
              (weakTimestamp === null || commitTimestamp >= weakTimestamp)
            ) {
              weakTimestamp = commitTimestamp
              weakCandidate = state
            } else if (weakestCandidate === null) {
              weakestCandidate = state
            }
          }
        }
      }
      return (
        strongCandidate ?? weakCandidate ?? weakestCandidate ?? Object.values(symbol.states)[0][0]
      )
    },
    findKey(symbol: SymbolDto, wantedTimestamp: number): KeyDto {
      let weakTimestamp: number | null = null
      let strongCandidate: KeyDto | null = null
      let weakCandidate: KeyDto | null = null
      for (const key of symbol.keys) {
        const keyTimestamp = new Date(key.from).valueOf()
        if (keyTimestamp === wantedTimestamp) {
          strongCandidate = key
        } else if (
          keyTimestamp < wantedTimestamp &&
          (weakTimestamp === null || keyTimestamp >= weakTimestamp)
        ) {
          weakTimestamp = keyTimestamp
          weakCandidate = key
        }
      }
      return strongCandidate ?? weakCandidate ?? symbol.keys[0]
    },
    findParentCrumbs(key: KeyDto, timestamp: number | null = null): KeyDto[] {
      if (key.parent == null) {
        return []
      }
      if (timestamp === null) {
        timestamp = new Date(key.from).valueOf()
      }
      const parentKey = this.findKey(this.root.symbols[key.parent], timestamp)
      const parents = this.findParentCrumbs(parentKey, timestamp)
      parents.push(key)
      return parents
    },
    getLastChangeDate(symbolId: number): Date {
      return normalizeDate(new Date(this.root.symbols[symbolId].keys.at(-1)!.from))
    },
    getPriorState(state: StateDto, symbolId: number): StateDto | null {
      if (state.cause === ChangeCause.ADDED || state.cause === ChangeCause.DELETED) {
        return null
      }
      const symbol = this.root.symbols[symbolId]
      const yearMonths = Object.keys(symbol.states)
      yearMonths.sort()
      const candidates = new Map(
        yearMonths
          .flatMap((ym) => symbol.states[ym] as StateDto[])
          .filter((s) => s.commit < state.commit)
          .map((s) => [s.commit, s]),
      )
      let commit = this.getCommit(state.commit)
      while (commit.parents.length > 0) {
        const parentIndex = commit.parents[0]
        if (candidates.has(parentIndex)) {
          return candidates.get(parentIndex)!
        }
        commit = this.getCommit(parentIndex)
      }
      return null
    },
  },
  getters: {
    metadata: (state) => state.root.meta,
  },
})
