import type { CommitDto } from "@/models/analyzer/CommitDto"
import type { SymbolDto } from "@/models/analyzer/SymbolDto"
import type { IndexRootDto } from "@/models/analyzer/IndexRootDto"
import type { MetaDto } from "@/models/analyzer/MetaDto"

export interface RootDto {
  meta: MetaDto;
  commits: CommitDto[];
  symbols: SymbolDto[];
  indices: IndexRootDto;
}
