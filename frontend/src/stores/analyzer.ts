import { defineStore } from "pinia"
import type { RootDto } from "@/models/analyzer"
import Fuse from "fuse.js"
import { fuseOptions } from "@/constants/fuse-options"

export const useAnalyzerStore = defineStore('analyzer', {
  state: () => ({
    root: null as unknown as RootDto,
    loading: false,
    error: null as string | null,
    globalFuse: null as unknown as Fuse<[string, number[]]>
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
        const rawKeysToIds: Record<string, number[]> = {}
        for (const symbol of this.root.symbols) {
          for (const key of symbol.keys) {
            const array = rawKeysToIds[key.name] ?? []
            if (array.length === 0) {
              rawKeysToIds[key.name] = array
            }
            array.push(symbol.id)
          }
        }
        this.globalFuse = new Fuse(Object.entries(rawKeysToIds), fuseOptions({
          keys: [
            {
              name: 'name',
              getFn: entry => entry[0],
            },
          ],
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
    search(query: string) {
      return this.globalFuse.search(query).flatMap((result) => {
        return result.item[1].map(i => ({
          symbol: this.root.symbols[i]
        }))
      })
    },
  },
  getters: {
    metadata: (state) => state.root.meta,
  },
})
