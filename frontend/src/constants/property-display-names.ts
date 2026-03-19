import type { PropertyKey } from '@/models/analyzer'

export const PROPERTY_DISPLAY_NAMES: Record<PropertyKey, string> = {
  _level: 'Visualizer',
  annotations: 'Annotations',
  body: 'Method body',
  enumArguments: 'Enum arguments',
  initialValue: 'Initial value',
  kind: 'Symbol kind',
  lines: 'Lines',
  modifiers: 'Modifiers',
  parent: 'Parent symbol',
  path: 'Path',
  realizations: 'Implements',
  simpleName: 'Name',
  spoonPath: 'Unique path',
  supertypes: 'Extends',
  typeParameters: 'Type parameters',
  type: 'Type',
  visibility: 'Visibility',
}

const DISPLAY_PROPERTIES = [
  'annotations',
  'body',
  'enumArguments',
  'initialValue',
  'kind',
  'lines',
  'modifiers',
  'path',
  'realizations',
  'simpleName',
  'supertypes',
  'typeParameters',
  'type',
  'visibility',
] as const

export type DisplayPropertyKey = (typeof DISPLAY_PROPERTIES)[number]

export const PROPERTIES_TO_DISPLAY = new Set<PropertyKey>(DISPLAY_PROPERTIES)
