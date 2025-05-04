export interface CommitDto {
  id: number;
  hash: string;
  date: string;
  summary: string;
  desc: string;
  parents: number[];
}
