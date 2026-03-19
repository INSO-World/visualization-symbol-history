import type { TypeArgument } from '@/models/analyzer/value/TypeArgument'
import type { QualifiedName } from '@/models/analyzer/common'

export type Type = UnknownType

export interface UnknownType {
  qualifiedName: QualifiedName
  typeArguments?: TypeArgument[]
}
