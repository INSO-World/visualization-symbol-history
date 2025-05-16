import type { KeyDto } from "@/models/KeyDto"
import type { StateDto } from "@/models/StateDto"
import type { RawYearMonth } from "@/models/common"

export interface SymbolDto {
  id: number;
  deleted: boolean;
  keys: KeyDto[];
  states: Record<RawYearMonth, StateDto[]>;
}
