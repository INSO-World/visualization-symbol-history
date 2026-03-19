import type { Kind } from '@/models/analyzer/value/Kind'
import type { RawZonedDateTime } from '@/models/analyzer/common'

type ParentDto = number | null

export interface KeyDto {
  parent: ParentDto
  from: RawZonedDateTime
  to?: RawZonedDateTime
  name: string
  kind: Kind
}
