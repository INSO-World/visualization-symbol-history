import type { KeyDto } from "@/models/analyzer/KeyDto"
import type { StateDto } from "@/models/analyzer/StateDto"
import type { RawYearMonth, RawZonedDateTime } from '@/models/analyzer/common'

export interface SymbolDto {
  id: number;
  deletedAt?: RawZonedDateTime;
  keys: KeyDto[];
  states: Record<RawYearMonth, StateDto[]>;
}
