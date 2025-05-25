import type { Chip } from '@/models/Chip'
import type { SymbolDto } from "@/models/analyzer"

export type SymbolElement = {
  result: SymbolDto
  header: string
  icon: string
  name: string
  suffix?: string
  highlights: ReadonlyArray<[number, number]>
  chips: Chip[]
  score: number
  deleted: boolean
}
