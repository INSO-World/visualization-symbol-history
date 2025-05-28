import { ChangeCause, type StateDto, type StateField, UpdateFlag } from '@/models/analyzer'
import { CellEventCategory, EventFlag } from '@/models/Cell'

const UPDATE_FLAG_MAPPING: Record<UpdateFlag, EventFlag | undefined> = {
  BODY_UPDATED: EventFlag.BODY,
  MOVED: EventFlag.MOVED,
  MOVED_WITH_PARENT: undefined,
  RENAMED: EventFlag.RENAMED,
  REORDERED: EventFlag.REORDERED,
  REPLACED: EventFlag.REPLACED,
}

export function getEventFlags(state: StateDto): Set<EventFlag> {
  const result = new Set<EventFlag>()
  if (state.cause === ChangeCause.ADDED) {
    result.add(EventFlag.ADDED)
  } else if (state.cause === ChangeCause.DELETED) {
    result.add(EventFlag.DELETED)
  } else if (state.cause === ChangeCause.CHANGED) {
    // Do nothing
  } else {
    result.add(EventFlag.BRANCHED)
  }
  if (state.flags != null) {
    const eventFlags = new Set<EventFlag>(
      state.flags.map((f) => UPDATE_FLAG_MAPPING[f]).filter((f) => f != null),
    )
    if (state.flags.includes(UpdateFlag.MOVED_WITH_PARENT)) {
      eventFlags.delete(EventFlag.MOVED)
    }
    eventFlags.forEach((f) => result.add(f))
  }
  const updatedSet = new Set<StateField>(state.updated)
  if (updatedSet.has('initialValue') || updatedSet.has('enumArguments')) {
    result.add(EventFlag.VALUE)
  }
  if (updatedSet.has('annotations')) {
    result.add(EventFlag.ANNOTATIONS)
  }
  if (updatedSet.has('kind')) {
    result.add(EventFlag.KIND)
  }
  if (updatedSet.has('modifiers')) {
    result.add(EventFlag.MODIFIERS)
  }
  if (updatedSet.has('realizations')) {
    result.add(EventFlag.REALIZATIONS)
  }
  if (updatedSet.has('supertypes')) {
    result.add(EventFlag.SUPERTYPES)
  }
  if (updatedSet.has('typeParameters')) {
    result.add(EventFlag.TYPE_PARAMETERS)
  }
  if (updatedSet.has('type')) {
    result.add(EventFlag.TYPE)
  }
  if (updatedSet.has('visibility')) {
    result.add(EventFlag.VISIBILITY)
  }
  return result
}

const MAJOR_EVENT_FLAGS = new Set<EventFlag>([
  EventFlag.REPLACED,
  EventFlag.ANNOTATIONS,
  EventFlag.KIND,
  EventFlag.MOVED,
  EventFlag.RENAMED,
  EventFlag.MODIFIERS,
  EventFlag.REALIZATIONS,
  EventFlag.SUPERTYPES,
  EventFlag.TYPE_PARAMETERS,
  EventFlag.TYPE,
  EventFlag.VISIBILITY,
])
const MINOR_EVENT_FLAGS = new Set<EventFlag>([
  EventFlag.BRANCHED,
  EventFlag.VALUE,
  EventFlag.REORDERED,
  EventFlag.BODY,
])

export function getCellEventCategory(flag: EventFlag): CellEventCategory {
  if (flag === EventFlag.DELETED) {
    return CellEventCategory.DELETED
  }
  if (flag === EventFlag.ADDED) {
    return CellEventCategory.ADDED
  }
  if (MAJOR_EVENT_FLAGS.has(flag)) {
    return CellEventCategory.MAJOR
  }
  if (MINOR_EVENT_FLAGS.has(flag)) {
    return CellEventCategory.MINOR
  }
  return CellEventCategory.MINISCULE
}
