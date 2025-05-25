import type { SymbolElement } from '@/models/SymbolElement'
import type { SymbolEvent } from '@/models/SymbolEvent'
import type { DateObject } from "@/models/DateObject"
import { toDateObject } from "@/models/DateObject"

export const elements: Omit<SymbolElement, 'result' | 'score' | 'deleted'>[] = [
  {
    header: 'at.am307.solver.core.Page',
    icon: 'constant',
    name: 'ELEMENTS_PER_PAGE',
    highlights: [[0, 4]],
    suffix: ': int',
    chips: [{ username: 'AM307', percentage: 100 }],
  },
  {
    header: 'at.am307.solver.util',
    icon: 'class',
    name: 'Telemetry',
    highlights: [[1, 5]],
    chips: [{ username: 'AM307', percentage: 100 }],
  },
  {
    header: 'at.am307.solver.feature.elimination.Strategy',
    icon: 'parameter',
    name: 'element',
    highlights: [[0, 4]],
    suffix: ': SolveResult',
    chips: [{ username: 'AM307', percentage: 100 }],
  },
]

export const symbolEvents: SymbolEvent[][] = [
  [
    {
      event: 'added',
      date: new Date('2024-07-01Z'),
      authors: ['AM307'],
    },
  ],
  [
    {
      event: 'added',
      date: new Date('2024-07-06Z'),
      authors: ['AM307'],
    },
    {
      event: 'rename',
      date: new Date('2024-07-15Z'),
      authors: ['AM307'],
    },
  ],
  [
    {
      event: 'added',
      date: new Date('2024-07-06Z'),
      authors: ['AM307', 'torvalds'],
    },
    {
      event: 'deleted',
      date: new Date('2024-07-12Z'),
      authors: ['torvalds'],
    },
  ],
]

export function addDays(date: Date, days: number): Date {
  const result = new Date(date)
  result.setDate(result.getDate() + days)
  return result
}

export const startDate = new Date('2024-07-01Z')
const dates: Date[] = []
for (let i = 0; i < 20; i++) {
  dates.push(addDays(startDate, i))
}

export const dateObjects: DateObject[] = dates.map(toDateObject)
