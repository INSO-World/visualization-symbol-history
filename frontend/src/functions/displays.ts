import {
  type Expression,
  type PropertyMapping,
  type UnknownType,
  type UpperTypeBound,
  Visibility,
} from '@/models/analyzer'
import { KIND_MAPPING, typeToText } from '@/functions/analyzer'
import type { DisplayPropertyKey } from '@/constants/property-display-names'
import { capitalize } from '@/functions/lang'

function longType(type: UnknownType): Display {
  return {
    text: typeToText(type, Infinity, true),
    abbr: type.qualifiedName,
  }
}

function shortType(type: UnknownType): Display {
  return {
    text: typeToText(type, Infinity, false),
    abbr: type.qualifiedName,
  }
}

function bound(bound?: UpperTypeBound): string {
  return bound != null ? ` extends ${shortType(bound.extends).text}` : ''
}

const QUALIFIED_PREFIX_REGEX = /(?<![a-z]\.)(?<=[. (\[]|^)(?:[a-z\d]+\.)+/g

function expression(expression: Expression): Display {
  const short = expression.replace(QUALIFIED_PREFIX_REGEX, '')
  if (short.length <= 20) {
    return {
      text: short,
    }
  }
  return {
    text: "(Longer expression)",
    abbr: short,
  }
}

export type Display = {
  text: string
  abbr?: string
}

type DisplayMapping<R> = {
  [K in DisplayPropertyKey]: (key: PropertyMapping[K]) => R
}

export function asDisplayArray(display: string | string[] | Display | Display[]): Display[] {
  if (typeof display === 'string') {
    return [{ text: display }]
  }
  if (!Array.isArray(display)) {
    return [display]
  }
  if (typeof display[0] === 'string') {
    return (display as string[]).map(text => ({ text }))
  }
  return display as Display[]
}

export const PROPERTY_DISPLAYS: DisplayMapping<string | string[] | Display | Display[]> = {
  annotations(types) {
    if (types == null) {
      return '(All annotations removed)'
    }
    return types.map(shortType).map((d) => ({
      text: `@${d.text}`,
      abbr: d.abbr,
    }))
  },
  body: (_hash) => {
    return _hash == null ? '(Removed)' : 'Structure changed'
  },
  enumArguments(args) {
    return args == null ? '(Removed)' : args.map(expression)
  },
  initialValue(val) {
    if (val == null) {
      return '(No longer initialized)'
    }
    return expression(val)
  },
  kind(kind) {
    if (kind == null) {
      return '(Unknown)'
    }
    return capitalize(KIND_MAPPING[kind].text)
  },
  lines: (lines) => {
    if (lines == null) {
      return '(Unknown)'
    }
    const [from, to] = lines
    if (from === to) {
      return from.toString(10)
    }
    return `${from}–${to}`
  },
  modifiers(modifiers) {
    if (modifiers == null) {
      return '(All modifiers removed)'
    }
    return modifiers.map((m) => m.toLowerCase()).join(' ')
  },
  path(path) {
    if (path == null) {
      return '(Unknown)'
    }
    const parts = path.split('#').slice(0, -1).flatMap(seg => {
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
    })
    const fullPath = parts.join('').slice(1)
    if (parts.length <= 4) {
      return fullPath
    }
    return {
      text: parts.slice(parts.length - 4, parts.length).join('').slice(1),
      abbr: fullPath,
    }
  },
  realizations(interfaces) {
    if (interfaces == null) {
      return '(None)'
    }
    return interfaces.map(longType)
  },
  simpleName(name) {
    return name ?? '(Unknown)'
  },
  supertypes(supertypes) {
    if (supertypes == null) {
      return '(None)'
    }
    return supertypes.map(longType)
  },
  typeParameters(typeParameters) {
    if (typeParameters == null) {
      return '(No longer generic)'
    }
    return typeParameters.map((p) => `${p.name}${bound(p.bound)}`).join(', ')
  },
  type(type) {
    if (type == null) {
      return '(No longer typed)'
    }
    return longType(type)
  },
  visibility(visibility) {
    if (visibility == null) {
      return '(No longer a type or type member)'
    }
    if (visibility === Visibility.PACKAGE_PRIVATE) {
      return "default (package private)"
    }
    return visibility.toLowerCase()
  },
}
