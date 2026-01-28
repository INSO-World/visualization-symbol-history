import { EventFlag } from '@/models/analyzer'

export const EVENT_FLAG_NAMES: Record<EventFlag, string> = {
  none: "-",
  added: "Added",
  annotations: "Annotations",
  body: "Method body",
  branched: "Branched",
  deleted: "Deleted",
  kind: "Kind",
  modifiers: "Modifiers",
  moved: "Moved",
  realizations: "Interfaces",
  renamed: "Renamed",
  reordered: "Re-ordered",
  replaced: "Replaced",
  supertypes: "Supertypes",
  type: "Type",
  type_parameters: "Type parameters",
  value: "Initial value",
  visibility: "Visibility",
}
