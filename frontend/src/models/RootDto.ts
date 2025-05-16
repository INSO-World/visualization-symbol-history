import type { CommitDto } from "@/models/CommitDto"
import type { SymbolDto } from "@/models/SymbolDto"
import type { IndexRootDto } from "@/models/IndexRootDto"
import type { MetaDto } from "@/models/MetaDto"

export interface RootDto {
  meta: MetaDto;
  commits: CommitDto[];
  symbols: SymbolDto[];
  indices: IndexRootDto;
}
