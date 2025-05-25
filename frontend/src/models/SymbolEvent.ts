import type { StateDto } from "@/models/analyzer"

export type SymbolEvent = {
  state: StateDto
  date: Date
  authors: string[]
}
