import type { Type } from '@/models/analyzer/value/Type'

export type TypeBound = UpperTypeBound | LowerTypeBound

export interface UpperTypeBound {
  extends: Type
}

export interface LowerTypeBound {
  super: Type
}
