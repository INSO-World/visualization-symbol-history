import type { Type } from "@/models/analyzer/value/Type"
import type { TypeBound } from "@/models/analyzer/value/TypeBound"

export type TypeArgument = ConcreteTypeArgument | WildcardTypeArgument;

export interface ConcreteTypeArgument {
  type: Type;
}

export interface WildcardTypeArgument {
  wildcard: 'any' | TypeBound;
}
