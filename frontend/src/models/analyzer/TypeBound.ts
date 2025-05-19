import type { Type } from "@/models/analyzer/Type"

export type TypeBound = UpperTypeBound | LowerTypeBound;

export interface UpperTypeBound {
  extends: Type;
}

export interface LowerTypeBound {
  super: Type;
}
