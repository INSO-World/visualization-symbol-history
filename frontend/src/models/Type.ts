import type { TypeArgument } from "@/models/TypeArgument"
import type { QualifiedName } from "@/models/common"

export type Type = UnknownType;

export interface UnknownType {
  qualifiedName: QualifiedName;
  typeArguments?: TypeArgument[];
}
