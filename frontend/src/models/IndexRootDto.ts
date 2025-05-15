import type { Visibility } from "@/models/Visibility"
import type { Kind } from "@/models/Kind"
import type { QualifiedName, RawYearMonth, SymbolId } from "@/models/common"

export type Index<K extends string> = Record<K, SymbolId[]>;

export interface IndexRootDto {
  byVisibility: Index<Visibility>;
  byKind: Index<Kind>;
  byType: Index<QualifiedName>;
  byExistence: Index<RawYearMonth>;
  byChanged: Index<RawYearMonth>;
}
