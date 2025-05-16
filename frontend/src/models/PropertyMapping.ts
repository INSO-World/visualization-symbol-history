import type { Kind } from "@/models/Kind"
import type { AnalyzerLevel } from "@/models/AnalyzerLevel"
import type { Type } from "@/models/Type"
import type { Modifier } from "@/models/Modifier"
import type { Visibility } from "@/models/Visibility"
import type { TypeParameter } from "@/models/TypeParameter"

type Expression = string;

export type PropertyMapping = Partial<{
  _level: AnalyzerLevel;
  annotations: Type[];
  body: number;
  enumArguments: Expression[];
  initialValue: Expression;
  kind: Kind;
  lines: [number, number];
  modifiers: Modifier[];
  parent: number;
  path: string;
  realizations: Type[];
  simpleName: string;
  supertypes: Type[];
  typeParameters: TypeParameter[];
  type: Type;
  visibility: Visibility;
}>
