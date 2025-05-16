import type { UpperTypeBound } from "@/models/TypeBound"

export interface TypeParameter {
  name: string;
  bound?: UpperTypeBound;
}
