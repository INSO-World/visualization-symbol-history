import { defineStore } from "pinia"
import type { CommitDto, KeyDto, RootDto, SymbolDto } from "@/models/analyzer"
import Fuse, { type FuseResult } from "fuse.js"
import { fuseOptions } from "@/constants/fuse-options"

type KeyRecord = {
  id: number
  keys: KeyDto[]
}

type FuseRecord = {
  name: string
} & KeyRecord;

export const useAnalyzerStore = defineStore('analyzer', {
  state: () => ({
    root: null as unknown as RootDto,
    loading: false,
    error: null as string | null,
    globalFuse: null as unknown as Fuse<FuseRecord>
  }),
  actions: {
    async init() {
      this.loading = true
      this.error = null
      try {
        this.root = await fetch('result.json')
          .then(res => {
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
        const fuseObject = [...rawKeysToIds.entries()].flatMap(e =>
          [...e[1].values()].map(r => ({
            name: e[0],
            ...r
          } satisfies FuseRecord))
        )
        console.log(fuseObject)
        this.globalFuse = new Fuse(fuseObject, fuseOptions({
          keys: ['name'],
        }))
      } catch (error) {
        if (!(error instanceof Error)) {
          throw error
        }
        this.error = error.message
        console.error("Error initializing analyzer store", error)
      } finally {
        this.loading = false
      }
    },
    commitDate(index: number): Date {
      return new Date(this.root.commits[index].date)
    },
    search(query: string) {
      return this.globalFuse.search(query).map((result) => {
        const record: KeyRecord = result.item
        return {
          ...result,
          item: {
            symbol: this.root.symbols[record.id],
            key: [...new Set(record.keys)][0],
          },
        }
      })
    },
  },
  getters: {
    metadata: (state) => state.root.meta,
  },
})
