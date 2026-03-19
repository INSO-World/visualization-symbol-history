import type { Chip } from '@/models/Chip'
import type { SymbolDto } from '@/models/analyzer'
import type { Range } from '@/models/common'

export type NameEntry = {
  from: {
    date: Date
    ts: number
  }
  to: {
    date?: Date
    ts: number
  }
  name: string
}

export type SymbolElement = {
  result: SymbolDto
  header: string
  kind: {
    name: string
    icon: string
  }
  path: string
  name: string
  allNames: NameEntry[]
  suffix?: string
  highlights: ReadonlyArray<Range>
  chips: Chip[]
  score: number
  createdAt: Date
  deletedAt?: Date
}
