import type { ChangeCause } from "@/models/ChangeCause"
import type { PropertyMapping } from "@/models/PropertyMapping"

export type StateDto = PureStateDto | ChangedStateDto;

interface BaseStateDto {
  cause: ChangeCause;
  commit: number;
  symbolId: number;
  updated?: string[];
  flags?: string[];
  properties: PropertyMapping;
}

export interface PureStateDto extends BaseStateDto {
  cause: ChangeCause.ADDED | ChangeCause.DELETED | ChangeCause.SUCCEEDED_PURE;
  updated: never;
  flags: never;
}

export interface ChangedStateDto extends BaseStateDto {
  cause: ChangeCause.CHANGED | ChangeCause.SUCCEEDED_CHANGED;
  updated: string[];
}
