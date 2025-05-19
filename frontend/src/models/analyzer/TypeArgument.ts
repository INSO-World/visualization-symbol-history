import type { Type } from "@/models/analyzer/Type"
import type { TypeBound } from "@/models/analyzer/TypeBound"

export type TypeArgument = ConcreteTypeArgument | WildcardTypeArgument;

export interface ConcreteTypeArgument {
  type: Type;
}

export interface WildcardTypeArgument {
  wildcard: 'any' | TypeBound;
}
