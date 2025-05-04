import type { CommitDto } from "@/models/CommitDto"
import type { SymbolDto } from "@/models/SymbolDto"

export interface RootDto {
  commits: CommitDto[];
  symbols: SymbolDto[];
}
