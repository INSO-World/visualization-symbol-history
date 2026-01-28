import type { RawZonedDateTime } from "@/models/analyzer/common"

export interface CommitDto {
  id: number;
  strand: number;
  hash: string;
  date: RawZonedDateTime;
  author: number;
  summary: string;
  desc?: string;
  parents: number[];
}
