import type { KeyDto } from '@/models/analyzer/KeyDto'
import type { StateDto } from '@/models/analyzer/StateDto'
import type { RawYearMonth, RawZonedDateTime } from '@/models/analyzer/common'
import type { ContributionDto } from '@/models/analyzer/ContributionDto'

export interface SymbolDto {
  id: number
  deletedAt?: RawZonedDateTime
  keys: KeyDto[]
  contributions: ContributionDto[]
  states: Record<RawYearMonth, StateDto[]>
}
