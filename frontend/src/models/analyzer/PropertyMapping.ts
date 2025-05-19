import type { Kind } from "@/models/analyzer/Kind"
import type { AnalyzerLevel } from "@/models/analyzer/AnalyzerLevel"
import type { Type } from "@/models/analyzer/Type"
import type { Modifier } from "@/models/analyzer/Modifier"
import type { Visibility } from "@/models/analyzer/Visibility"
import type { TypeParameter } from "@/models/analyzer/TypeParameter"

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
