import type { Kind } from "@/models/Kind"
import type { RawZonedDateTime } from "@/models/common"

type ParentDto = number;

export interface KeyDto {
  parent: ParentDto;
  from: RawZonedDateTime;
  to?: RawZonedDateTime;
  name: string;
  kind: Kind;
}
