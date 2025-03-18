import type { Chip } from '@/models/Chip'

export type SymbolElement = {
  header: string
  icon: string
  name: string
  suffix?: string
  highlight?: [number, number]
  chips: Chip[]
}
