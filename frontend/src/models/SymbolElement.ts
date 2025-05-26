import type { Chip } from '@/models/Chip'
import type { SymbolDto } from "@/models/analyzer"
import type { Range } from "@/models/common"

export type SymbolElement = {
  result: SymbolDto
  header: string
  kind: {
    name: string
    icon: string
  }
  path: string
  name: string
  suffix?: string
  highlights: ReadonlyArray<Range>
  chips: Chip[]
  score: number
  createdAt: Date
  deletedAt?: Date
}
