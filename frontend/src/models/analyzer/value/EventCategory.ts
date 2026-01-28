export enum EventCategory {
  MINISCULE = 1,
  MINOR,
  MAJOR,
  ADDED,
  DELETED,
}

export function maxCategory(...categories: EventCategory[]): EventCategory {
  return Math.max(...categories);
}
