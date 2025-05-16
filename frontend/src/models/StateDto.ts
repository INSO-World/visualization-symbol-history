import type { ChangeCause } from "@/models/ChangeCause"
import type { PropertyMapping } from "@/models/PropertyMapping"
import type { OriginDto } from "@/models/OriginDto"

export type StateDto = AdditionStateDto | DeletionStateDto | PureSuccessionStateDto | ChangeStateDto | ChangeSuccessionStateDto;

interface BaseStateDto {
  cause: ChangeCause;
  origins?: OriginDto[];
  commit: number;
  symbolId: number;
  updated?: string[];
  flags?: string[];
  properties: PropertyMapping;
}

type WithCause<T extends ChangeCause> = {
  cause: T;
}

type WithUpdates = {
  updated: string[];
}

type WithoutUpdates = {
  updated: never;
  flags: never;
}

type NoOrigins = {
  origins: never;
}

type OnlyOneOrigin = {
  origins: [OriginDto];
}

type ManyOrigins = {
  origins: OriginDto[];
}

type         AdditionStateDto = BaseStateDto & WithCause<ChangeCause.ADDED>             & WithoutUpdates & NoOrigins;
type         DeletionStateDto = BaseStateDto & WithCause<ChangeCause.DELETED>           & WithoutUpdates & OnlyOneOrigin;
type   PureSuccessionStateDto = BaseStateDto & WithCause<ChangeCause.SUCCEEDED_PURE>    & WithoutUpdates & ManyOrigins;
type           ChangeStateDto = BaseStateDto & WithCause<ChangeCause.CHANGED>           & WithUpdates    & OnlyOneOrigin;
type ChangeSuccessionStateDto = BaseStateDto & WithCause<ChangeCause.SUCCEEDED_CHANGED> & WithUpdates    & OnlyOneOrigin;
