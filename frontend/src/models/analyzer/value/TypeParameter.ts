import type { UpperTypeBound } from "@/models/analyzer/value/TypeBound"

export interface TypeParameter {
  name: string;
  bound?: UpperTypeBound;
}
