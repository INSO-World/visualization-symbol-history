import type { KeyDto } from "@/models/KeyDto"
import type { StateDto } from "@/models/StateDto"

export interface SymbolDto {
  id: number;
  deleted: boolean;
  keys: KeyDto[];
  states: Record<string, StateDto[]>;
}
