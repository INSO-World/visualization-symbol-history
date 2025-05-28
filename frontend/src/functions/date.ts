export function addDays(date: Date, days: number): Date {
  const result = new Date(date)
  result.setDate(result.getDate() + days)
  return normalizeDate(result)
}

export function normalizeDate(date: Date): Date {
  return new Date(date.toISOString().substring(0, 10))
}
