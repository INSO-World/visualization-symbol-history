import type { Visibility } from "@/models/analyzer/value/Visibility"
import type { Kind } from "@/models/analyzer/value/Kind"
import type { QualifiedName, RawYearMonth, SymbolId } from "@/models/analyzer/common"

export type Index<K extends string> = Record<K, SymbolId[]>;

export interface IndexRootDto {
  byVisibility: Index<Visibility>;
  byKind: Index<Kind>;
  byType: Index<QualifiedName>;
  byExistence: Index<RawYearMonth>;
  byChanged: Index<RawYearMonth>;
}
