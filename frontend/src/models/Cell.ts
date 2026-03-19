import type { CommitDto, StateDto } from '@/models/analyzer'
import type { EventFlag } from '@/models/analyzer/value/EventFlag'
import type { EventCategory } from '@/models/analyzer/value/EventCategory'

export const EVENT_FLAG_PILLS: Record<EventFlag, string> = {
  none: '-',
  added: 'ADD',
  annotations: 'ANO',
  body: 'BOD',
  branched: 'BRN',
  deleted: 'DEL',
  kind: 'KIND',
  modifiers: 'MOD',
  moved: 'MOV',
  realizations: 'INTF',
  renamed: 'REN',
  reordered: 'ORD',
  replaced: 'REPL',
  supertypes: 'SUP',
  type: 'TYPE',
  type_parameters: 'TPAR',
  value: 'VAL',
  visibility: 'VIS',
}

export type CellEvent = {
  category: EventCategory
  state: StateDto
  commit: CommitDto
  sourceCommits: CommitDto[]
  flags: Set<EventFlag>
  authors: string[]
  last: boolean
}

export type Cell = {
  events?: {
    list: CellEvent[]
    category: EventCategory
    flags: Set<EventFlag>
    mainFlag?: EventFlag
    hoverText: string
    authors: string[]
  }
  starts: boolean
  ends: boolean
  last: boolean
}

export type EventCell = Required<Cell>
