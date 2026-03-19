import type { CommitDto } from '@/models/analyzer/CommitDto'
import type { SymbolDto } from '@/models/analyzer/SymbolDto'
import type { IndexRootDto } from '@/models/analyzer/IndexRootDto'
import type { MetaDto } from '@/models/analyzer/MetaDto'
import type { AuthorDto } from '@/models/analyzer/AuthorDto'

export interface RootDto {
  meta: MetaDto
  authors: AuthorDto[]
  commits: CommitDto[]
  symbols: SymbolDto[]
  indices: IndexRootDto
}
