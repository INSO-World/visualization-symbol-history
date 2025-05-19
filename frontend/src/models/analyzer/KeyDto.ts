import type { Kind } from "@/models/analyzer/Kind"
import type { RawZonedDateTime } from "@/models/analyzer/common"

type ParentDto = number;

export interface KeyDto {
  parent: ParentDto;
  from: RawZonedDateTime;
  to?: RawZonedDateTime;
  name: string;
  kind: Kind;
}
