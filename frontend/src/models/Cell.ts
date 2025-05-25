import type { StateDto } from "@/models/analyzer"

export enum EventFlag {
  ADDED = 'added',
  DELETED = 'deleted',
  BRANCHED = 'branched',
  REPLACED = 'replaced',
  VALUE = 'value',
  ANNOTATIONS = 'annotations',
  KIND = 'kind',
  MOVED = 'move',
  RENAMED = 'name',
  REORDERED = 'order',
  BODY = 'body',
  MODIFIERS = 'modifiers',
  REALIZATIONS = 'realizations',
  SUPERTYPES = 'supertypes',
  TYPE_PARAMETERS = 'typeParameters',
  TYPE = 'type',
  VISIBILITY = 'visibility',
}

export type Cell = {
  event?: {
    flags: Set<EventFlag>
    state: StateDto
    authors: string[]
  }
  starts: boolean
  ends: boolean
}
