import type { UpperTypeBound } from "@/models/analyzer/TypeBound"

export interface TypeParameter {
  name: string;
  bound?: UpperTypeBound;
}
