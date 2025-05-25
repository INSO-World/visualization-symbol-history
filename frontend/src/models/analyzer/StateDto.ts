import type { ChangeCause } from "@/models/analyzer/value/ChangeCause"
import type { PropertyMapping } from "@/models/analyzer/value/PropertyMapping"
import type { OriginDto } from "@/models/analyzer/OriginDto"
import type { UpdateFlag } from "@/models/analyzer/value/UpdateFlag"

export type StateDto = AdditionStateDto | DeletionStateDto | PureSuccessionStateDto | ChangeStateDto | ChangeSuccessionStateDto;

export type StateField = keyof PropertyMapping;

interface BaseStateDto {
  cause: ChangeCause;
  origins?: OriginDto[];
  commit: number;
  symbolId: number;
  updated?: StateField[];
  flags?: UpdateFlag[];
  properties: PropertyMapping;
}

type WithCause<T extends ChangeCause> = {
  cause: T;
}

type WithUpdates = {
  updated: StateField[];
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
