import type { Kind } from "@/models/Kind"

type ParentDto = number;

export interface KeyDto {
  parent: ParentDto;
  from: string;
  to?: string;
  name: string;
  kind: Kind;
}
