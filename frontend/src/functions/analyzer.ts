import {
  type ConcreteTypeArgument,
  Kind,
  type TypeArgument,
  type TypeBound,
  type UnknownType,
  type UpperTypeBound,
} from '@/models/analyzer'

export function isConcreteTypeArg(a: TypeArgument): a is ConcreteTypeArgument {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return (a as any)['type'] != null
}

export function isUpperBound(b: TypeBound): b is UpperTypeBound {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return (b as any)['extends'] != null
}

type KindInfo = {
  icon: string
  abstract: boolean
  typed: boolean
  parameterized: boolean
  text: string
}
export const KIND_MAPPING: Record<Kind, KindInfo> = {
  ANNOTATION: {
    icon: 'annotation',
    abstract: false,
    typed: false,
    parameterized: false,
    text: 'annotation',
  },
  CLASS: {
    icon: 'class',
    abstract: true,
    typed: false,
    parameterized: false,
    text: 'class',
  },
  CONSTANT_FIELD: {
    icon: 'constant',
    abstract: false,
    typed: true,
    parameterized: false,
    text: 'class constant',
  },
  CONSTANT_VARIABLE: {
    icon: 'constant',
    abstract: false,
    typed: true,
    parameterized: false,
    text: 'local constant',
  },
  CONSTRUCTOR: {
    icon: 'constructor',
    abstract: false,
    typed: false,
    parameterized: true,
    text: 'constructor',
  },
  ENUM: {
    icon: 'enum',
    abstract: false,
    typed: false,
    parameterized: false,
    text: 'enum',
  },
  ENUM_CONSTANT: {
    icon: 'enum_constant',
    abstract: false,
    typed: false,
    parameterized: false,
    text: 'enum constant',
  },
  FIELD: {
    icon: 'field',
    abstract: false,
    typed: true,
    parameterized: false,
    text: 'field',
  },
  INTERFACE: {
    icon: 'interface',
    abstract: false,
    typed: false,
    parameterized: false,
    text: 'interface',
  },
  METHOD: {
    icon: 'method',
    abstract: true,
    typed: true,
    parameterized: true,
    text: 'method',
  },
  MODULE: {
    icon: 'module',
    abstract: false,
    typed: false,
    parameterized: false,
    text: 'module',
  },
  PACKAGE: {
    icon: 'package',
    abstract: false,
    typed: false,
    parameterized: false,
    text: 'package',
  },
  PARAMETER: {
    icon: 'parameter',
    abstract: false,
    typed: true,
    parameterized: false,
    text: 'parameter',
  },
  RECORD: {
    icon: 'record',
    abstract: false,
    typed: false,
    parameterized: false,
    text: 'record class',
  },
  VARIABLE: {
    icon: 'variable',
    abstract: false,
    typed: true,
    parameterized: false,
    text: 'variable',
  },
}

export function typeToText(t: UnknownType, limit: number, deep: boolean = true): string {
  let result = t.qualifiedName.replace(/\$/g, '.').split('.').at(-1)!
  if (deep && t.typeArguments != null) {
    const parts = t.typeArguments.map((arg) => {
      if (isConcreteTypeArg(arg)) {
        return typeToText(arg.type, limit, false)
      } else if (arg.wildcard === 'any') {
        return '?'
      } else if (isUpperBound(arg.wildcard)) {
        return `? extends ${typeToText(arg.wildcard.extends, limit, false)}`
      } else {
        return `? super ${typeToText(arg.wildcard.super, limit, false)}`
      }
    })
    result += '<'
    for (let i = 0; i < parts.length; i++) {
      if (i > 0) {
        result += ', '
      }
      const part = parts[i]
      if (result.length + part.length + 2 > limit) {
        result += '…'
        break
      }
      result += part
    }
    result += '>'
  }
  return result
}
