import type { KeyDto } from "@/models/analyzer/KeyDto"
import type { StateDto } from "@/models/analyzer/StateDto"
import type { RawYearMonth } from "@/models/analyzer/common"

export interface SymbolDto {
  id: number;
  deleted: boolean;
  keys: KeyDto[];
  states: Record<RawYearMonth, StateDto[]>;
}
