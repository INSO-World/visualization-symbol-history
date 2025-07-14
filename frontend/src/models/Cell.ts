import type { CommitDto, StateDto } from "@/models/analyzer"

export enum EventFlag {
  ADDED = 'added',
  DELETED = 'deleted',
  BRANCHED = 'branched',
  REPLACED = 'replaced',
  VALUE = 'value',
  ANNOTATIONS = 'annotations',
  KIND = 'kind',
  MOVED = 'moved',
  RENAMED = 'renamed',
  REORDERED = 'reordered',
  BODY = 'body',
  MODIFIERS = 'modifiers',
  REALIZATIONS = 'realizations',
  SUPERTYPES = 'supertypes',
  TYPE_PARAMETERS = 'typeParameters',
  TYPE = 'type',
  VISIBILITY = 'visibility',
}

export const EVENT_FLAG_PILLS: Record<EventFlag, string> = {
  added: "ADD",
  annotations: "ANO",
  body: "BOD",
  branched: "BRN",
  deleted: "DEL",
  kind: "KIND",
  modifiers: "MOD",
  moved: "MOV",
  realizations: "INTF",
  renamed: "REN",
  reordered: "ORD",
  replaced: "REPL",
  supertypes: "SUP",
  type: "TYPE",
  typeParameters: "TPAR",
  value: "VAL",
  visibility: "VIS",
}

export enum CellEventCategory {
  MINISCULE = 1,
  MINOR,
  MAJOR,
  ADDED,
  DELETED,
}

export type CellEvent = {
  category: CellEventCategory
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
    category: CellEventCategory
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
