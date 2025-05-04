import type { TypeArgument } from "@/models/TypeArgument"

export type Type = UnknownType;

export interface UnknownType {
  qualifiedName: string;
  typeArguments?: TypeArgument[];
}
