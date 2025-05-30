import { type FullPropertyMapping, type UnknownType, type UpperTypeBound, Visibility } from '@/models/analyzer'
import { KIND_MAPPING, typeToText } from '@/functions/analyzer'
import type { DisplayPropertyKey } from '@/constants/property-display-names'
import { capitalize } from '@/functions/lang'

function longType(type: UnknownType): string {
  return typeToText(type, Infinity, true)
}

function shortType(type: UnknownType): string {
  return typeToText(type, Infinity, false)
}

function bound(bound?: UpperTypeBound): string {
  return bound != null ? ` extends ${shortType(bound.extends)}` : ''
}

export const PROPERTY_DISPLAYS: {
  [K in DisplayPropertyKey]: (key: FullPropertyMapping[K]) => string[]
} = {
  annotations(types) {
    return types.map(shortType).map((s) => `@${s}`)
  },
  body: (_hash) => ['Structure changed'],
  enumArguments(args) {
    return args
  },
  initialValue(val) {
    return [val.length <= 20 ? val : '(Longer expression)']
  },
  kind(kind) {
    return [capitalize(KIND_MAPPING[kind].text)]
  },
  lines: ([from, to]) => {
    if (from === to) {
      return [from.toString(10)]
    }
    return [`${from}–${to}`]
  },
  modifiers(modifiers) {
    return [modifiers.map((m) => m.toLowerCase()).join(' ')]
  },
  path(path) {
    return [path.split('#').slice(0, -1).flatMap(seg => {
      const name = seg.indexOf('name=')
      const sig = seg.indexOf('signature=')
      if (name === -1 && sig === -1) {
        return [] as string[]
      }
      if (name !== -1) {
        const end = seg.indexOf(']')
        return ['.' + seg.slice(name + 5, end)]
      }
      const paren = seg.indexOf('(')
      return ['#' + seg.slice(sig + 10, paren)]
    }).join('').slice(1)]
  },
  realizations(interfaces) {
    return interfaces.map(longType)
  },
  simpleName(name) {
    return [name]
  },
  supertypes(supertypes) {
    return supertypes.map(longType)
  },
  typeParameters(typeParameters) {
    return [typeParameters.map((p) => `${p.name}${bound(p.bound)}`).join(', ')]
  },
  type(type) {
    return [longType(type)]
  },
  visibility(visibility) {
    if (visibility === Visibility.PACKAGE_PRIVATE) {
      return ["default (package private)"]
    }
    return [visibility.toLowerCase()]
  },
}
