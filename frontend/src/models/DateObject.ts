export type DateObject = {
  date: Date
  weekday: string
  month: string
  day: string
}

const weekdayFormatter = new Intl.DateTimeFormat('en-US', { weekday: 'long' })
const monthFormatter = new Intl.DateTimeFormat('en-US', { month: 'long' })
const dayFormatter = new Intl.DateTimeFormat('en-US', { day: 'numeric' })

export function toDateObject(date: Date): DateObject {
  const weekday = weekdayFormatter.format(date).substring(0, 3).toUpperCase()
  const month = monthFormatter.format(date).substring(0, 3).toUpperCase()
  const day = dayFormatter.format(date)
  return { date, weekday, month, day }
}
