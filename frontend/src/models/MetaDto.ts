import type { RawZonedDateTime } from "@/models/common"

export interface MetaDto {
  name: string;
  createdAt: RawZonedDateTime;
  updatedAt: RawZonedDateTime;
  indexedAt: RawZonedDateTime;
  commitCount: number;
  strandSymbolCount: number;
  symbolCount: number;
  strandCount: number;
}
