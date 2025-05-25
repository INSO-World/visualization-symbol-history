import type { Kind } from "@/models/analyzer/value/Kind"
import type { AnalyzerLevel } from "@/models/analyzer/value/AnalyzerLevel"
import type { Type } from "@/models/analyzer/value/Type"
import type { Modifier } from "@/models/analyzer/value/Modifier"
import type { Visibility } from "@/models/analyzer/value/Visibility"
import type { TypeParameter } from "@/models/analyzer/value/TypeParameter"

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
