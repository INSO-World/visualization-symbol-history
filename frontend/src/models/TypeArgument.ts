import type { Type } from "@/models/Type"
import type { TypeBound } from "@/models/TypeBound"

export type TypeArgument = ConcreteTypeArgument | WildcardTypeArgument;

export interface ConcreteTypeArgument {
  type: Type;
}

export interface WildcardTypeArgument {
  wildcard: 'any' | TypeBound;
}
