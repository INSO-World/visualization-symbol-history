<!--suppress HtmlRequiredAltAttribute -->
<script setup lang="ts">
import { computed, ref, shallowRef, triggerRef } from 'vue'
import { type Cell, type CellEvent, EVENT_FLAG_PILLS, type EventCell } from '@/models/Cell'
import type { SymbolEvent } from '@/models/SymbolEvent'
import { toDateObject } from '@/models/DateObject'
import type { SymbolElement } from '@/models/SymbolElement'
import {
  ChangeCause,
  EVENT_FLAG_CATEGORIES,
  EventCategory,
  EventFlag,
  Kind,
  maxCategory,
  type PropertyKey,
  type StateDto,
  UpdateFlag,
} from '@/models/analyzer'
import { useAnalyzerStore } from '@/stores/analyzer'
import HighlightedText from '@/components/HighlightedText.vue'
import debounce from 'debounce'
import { addDays, normalizeDate } from '@/functions/date'
import { setOf, union } from '@/functions/lang'
import { resultToElement } from '@/functions/element'
import { EVENT_FLAG_NAMES } from '@/functions/event-flags'
import AnPopover from '@/components/AnPopover.vue'
import {
  type DisplayPropertyKey,
  PROPERTIES_TO_DISPLAY,
  PROPERTY_DISPLAY_NAMES,
} from '@/constants/property-display-names'
import {
  asDisplayArray,
  type Display,
  NONE_VALUE_TEXTS,
  PROPERTY_DISPLAYS,
} from '@/functions/displays'
import ConditionalAbbr from '@/components/ConditionalAbbr.vue'
import { lcs_myers_linear_space } from '@algorithm.ts/lcs'

const analyzerStore = useAnalyzerStore()

const searchTerm = ref<string>('')

const DATE_SPAN = 18
const MIN_DATE = normalizeDate(new Date('2020-01-01Z'))
const TODAY = normalizeDate(new Date())
const MAX_DATE = addDays(TODAY, -DATE_SPAN + 1)

const startDate = ref(MAX_DATE)
const DATE_OFFSETS = Array.from({ length: DATE_SPAN }).map((_, i) => i)
const dateObjects = computed(() =>
  DATE_OFFSETS.map((i) => addDays(startDate.value, i)).map(toDateObject),
)
const yearMonths = computed(() => {
  const values = dateObjects.value.map(
    (d) => `${d.date.getFullYear()}-${(d.date.getMonth() + 101).toString(10).substring(1)}`,
  )
  const result = [...new Set(values)]
  result.sort()
  return result
})
const searchResults = ref([] as SymbolElement[])
const ELEMENT_SPAN = 6
const elements = computed(() =>
  searchResults.value.slice(startElementIndex.value, startElementIndex.value + ELEMENT_SPAN),
)
const startElementIndex = ref(0)
const cells = shallowRef([] as Cell[][])

let lastSearchTerm: string | null = null

const debouncedSearch = debounce(async () => {
  if (lastSearchTerm !== null && searchTerm.value === lastSearchTerm) {
    return
  }
  if (searchTerm.value === '') {
    searchResults.value = []
    updateView()
    return
  }
  if (searchTerm.value.length < 3) {
    return
  }
  startElementIndex.value = 0
  lastSearchTerm = searchTerm.value
  searchResults.value = analyzerStore
    .search(searchTerm.value)
    .filter((s) => s.score >= 0.8)
    .map((s) => resultToElement(s, analyzerStore))
  updateView()
}, 500)

function search() {
  debouncedSearch()
}

function emptySearchTerm() {
  debouncedSearch.clear()
  searchTerm.value = ''
  updateView()
}

function previousElementPage() {
  if (startElementIndex.value > 0) {
    startElementIndex.value = Math.max(startElementIndex.value - ELEMENT_SPAN, 0)
    updateView()
  }
}

function nextElementPage() {
  if (startElementIndex.value < searchResults.value.length - ELEMENT_SPAN) {
    startElementIndex.value = Math.min(
      startElementIndex.value + ELEMENT_SPAN,
      searchResults.value.length - ELEMENT_SPAN,
    )
    updateView()
  }
}

function previousTimes() {
  if (+startDate.value > +MIN_DATE) {
    const newStartDate = addDays(startDate.value, -DATE_SPAN)
    if (+newStartDate < +MIN_DATE) {
      startDate.value = MIN_DATE
    } else {
      startDate.value = newStartDate
    }
    updateView()
  }
}

function nextTimes() {
  if (+startDate.value < +MAX_DATE) {
    const newStartDate = addDays(startDate.value, DATE_SPAN)
    if (+newStartDate > +MAX_DATE) {
      startDate.value = MAX_DATE
    } else {
      startDate.value = newStartDate
    }
    updateView()
  }
}

function currentTimes() {
  if (+startDate.value != +MAX_DATE) {
    startDate.value = MAX_DATE
    updateView()
  }
}

function elementTimes(date: Date) {
  const normalizedDate = normalizeDate(date)
  if (+startDate.value != +normalizedDate) {
    startDate.value = normalizedDate
    updateView()
  }
}

function updateView() {
  console.group('Update')
  console.log(startDate.value)
  console.log(elements.value)
  console.groupEnd()
  const symbolEvents: SymbolEvent[][] = []
  for (const element of elements.value) {
    const lastDate: Date | null =
      element.deletedAt == null ? analyzerStore.getLastChangeDate(element.result.id) : null
    const events: SymbolEvent[] = []
    for (const yearMonth of yearMonths.value) {
      const ymEvents: StateDto[] | undefined = element.result.states[yearMonth]
      if (ymEvents == null || ymEvents.length === 0) {
        continue
      }
      events.push(
        ...ymEvents.flatMap((state) => {
          const { updated } = state
          if (updated != null && updated.length === 1 && updated[0] === 'spoonPath') {
            return []
          }
          const date = normalizeDate(analyzerStore.commitDate(state.commit))
          const authors = element.result.contributions.map((contribution) =>
            analyzerStore.getAuthorGitHubUsername(contribution.author),
          )

          // TODO: Remove mock data insertion
          if (
            state.updated != null &&
            state.updated.includes('path') &&
            date.getMonth() === 4 &&
            date.getDate() === 15 &&
            state.properties.simpleName === 'idCounter'
          ) {
            authors.splice(0, 1, analyzerStore.mockUsernameSecondary)
          }

          return [
            {
              state,
              date,
              authors,
              last: lastDate != null && +lastDate === +date,
            },
          ]
        }),
      )
    }
    symbolEvents.push(events)
  }

  const started: boolean[] = symbolEvents.map((_a, i) => {
    const element = elements.value[i]
    if (element.deletedAt != null && +element.deletedAt < +startDate.value) {
      return false
    }
    return +element.createdAt < +startDate.value
  })

  const eventsToProcess = [
    ...Map.groupBy(
      symbolEvents.flatMap((a, i) => a.map((e) => ({ index: i, ...e }))),
      (e: SymbolEvent & { index: number }) => normalizeDate(e.date).valueOf(),
    ),
  ].map(([date, entries]) => ({ date, entries }))
  eventsToProcess.sort((a, b) => a.date - b.date)
  while (eventsToProcess.length > 0 && +eventsToProcess[0].date < +startDate.value) {
    eventsToProcess.shift()
  }
  const displayedMaxDate = addDays(startDate.value, DATE_SPAN)
  while (eventsToProcess.length > 0 && +eventsToProcess.at(-1)!.date > +displayedMaxDate) {
    eventsToProcess.pop()
  }
  const newCells: Cell[][] = []
  for (let i = 0; i < dateObjects.value.length; i++) {
    const column: Cell[] = []
    const { date } = dateObjects.value[i]
    const dateEvents = []
    if (eventsToProcess.length > 0 && +eventsToProcess[0].date === +date) {
      dateEvents.push(...eventsToProcess.shift()!.entries)
    }
    for (let e = 0; e < symbolEvents.length; e++) {
      const events = dateEvents.filter((ev) => ev.index === e)
      const element = elements.value[e]
      const createdAtTimestamp = normalizeDate(element.createdAt).valueOf()
      const deletedAtTimestamp =
        element.deletedAt != null ? normalizeDate(element.deletedAt).valueOf() : Infinity
      if (events.length > 0) {
        events.sort((a, b) => +a.date - +b.date)
        const list: CellEvent[] = events.map((ev) => {
          const flags = new Set(ev.state.events || [])
          const category = EVENT_FLAG_CATEGORIES[ev.state.mainEvent]
          return {
            category,
            state: ev.state,
            commit: analyzerStore.getCommit(ev.state.commit),
            sourceCommits: (ev.state.origins ?? [])
              .map((o) => o.sourceCommit)
              .map((c) => analyzerStore.getCommit(c)),
            flags,
            authors: ev.authors,
            last: ev.last,
          }
        })
        const mainCategory = maxCategory(...list.map((ev) => ev.category))
        const allFlags = union(list.map((ev) => ev.flags))
        const mainFlagCandidates: EventFlag[] = list
          .map((ev) => ev.state.mainEvent)
          .filter((f) => EVENT_FLAG_CATEGORIES[f] === mainCategory)
        const mainFlag = mainFlagCandidates.at(-1)
        const hoverText =
          mainCategory === EventCategory.MINISCULE ? 'Minor changes' : EVENT_FLAG_NAMES[mainFlag!]
        const cell: EventCell = {
          events: {
            list,
            category: mainCategory,
            flags: allFlags,
            mainFlag,
            hoverText,
            authors: setOf(list.flatMap((ev) => ev.authors)),
          },
          starts: false,
          ends: false,
          last: list.some((ev) => ev.last),
        }
        if (cell.events.flags.has(EventFlag.ADDED) && +date === createdAtTimestamp) {
          started[e] = true
          cell.starts = true
        }
        if (cell.events.flags.has(EventFlag.DELETED) && +date === deletedAtTimestamp) {
          started[e] = false
          cell.ends = true
        }
        column.push(cell)
      } else {
        const running = started[e]
        column.push({ starts: !running, ends: !running, last: false })
      }
    }
    newCells.push(column)
  }
  cells.value = newCells
  triggerRef(cells)
}

function iconNeedsPadding(icon: string): boolean {
  return ['field_injected', 'field', 'constant'].includes(icon)
}

function cellHash(cell: Cell, rowIndex: number, columnIndex: number): string {
  const prefix = `${columnIndex}/${rowIndex}~`
  let suffix: string
  if (cell.events == null) {
    if (cell.starts && cell.ends) {
      suffix = 'o'
    } else if (cell.starts) {
      suffix = '<'
    } else if (cell.ends) {
      suffix = '>'
    } else {
      suffix = '='
    }
  } else {
    suffix = cell.events.list.map((e) => `${e.state.symbolId}@${e.state.commit}`).join(' ')
  }
  return prefix + suffix
}

function columnHash(cells: Cell[], columnIndex: number): string {
  return `${columnIndex}\n\n${cells.map((cell, rowIndex) => cellHash(cell, rowIndex, columnIndex)).join('\n')}`
}

function getDisplayProperties(
  state: StateDto,
  rowIndex: number,
): Array<{ key: DisplayPropertyKey; name: string; values: Display[] }> {
  if (state.cause === ChangeCause.SUCCEEDED_PURE) {
    // In case of pure successions, there is no difference information
    return []
  }
  const symbolId = elements.value[rowIndex].result.id
  const priorState = analyzerStore.getPriorState(state, symbolId)
  const endState = state.cause === ChangeCause.ADDED || state.cause === ChangeCause.DELETED
  const updatedKeys: PropertyKey[] = endState
    ? (Object.keys(state.properties).filter((k) => k !== 'body') as PropertyKey[])
    : (state.updated ?? [])
  const keys = updatedKeys.filter((key) => PROPERTIES_TO_DISPLAY.has(key)) as DisplayPropertyKey[]
  return keys
    .map((key) => {
      let name = PROPERTY_DISPLAY_NAMES[key]
      if (key === 'type' && state.properties.kind === Kind.METHOD) {
        name = 'Return ' + name.toLowerCase()
      }
      let newDisplayArray = asDisplayArray(PROPERTY_DISPLAYS[key](state.properties[key]! as never))
      if (priorState != null) {
        const oldPropertyValue = priorState.properties[key]
        if (typeof oldPropertyValue === 'undefined') {
          newDisplayArray.forEach((d) => (d.marker = 1))
        } else {
          const oldDisplayArray = asDisplayArray(PROPERTY_DISPLAYS[key](oldPropertyValue as never))
          if (oldDisplayArray.length === 1 && oldDisplayArray.length === newDisplayArray.length) {
            const { text: oldText, abbr: oldAbbr } = oldDisplayArray[0]
            const { text: newText, abbr: newAbbr } = newDisplayArray[0]

            // TODO: Remove hotfix for erroneous path change detection
            if (
              state.flags != null &&
              !state.flags.includes(UpdateFlag.MOVED) &&
              key === 'path' &&
              oldText === newText &&
              oldAbbr === newAbbr
            ) {
              return null
            }

            if (key === 'body') {
              newDisplayArray[0].text = newText
            } else {
              newDisplayArray[0].text = `${oldText} → ${newText}`
            }
            if (newAbbr != null && oldAbbr != null) {
              newDisplayArray[0].abbr = `${oldAbbr}\n→\n${newAbbr}`
            }
            newDisplayArray[0].marker = NONE_VALUE_TEXTS.has(newText) ? -1 : 0
          } else {
            const lcsIndices = lcs_myers_linear_space(
              oldDisplayArray.length,
              newDisplayArray.length,
              (i1, i2) => oldDisplayArray[i1].text === newDisplayArray[i2].text,
            )
            // Zipping along the LCS
            const updatedDisplayArray: Display[] = []
            let oldIndex = 0
            let newIndex = 0
            let lcsIndex = 0
            while (oldIndex < oldDisplayArray.length || newIndex < newDisplayArray.length) {
              const nextLcsPair = lcsIndex < lcsIndices.length ? lcsIndices[lcsIndex] : null
              if (
                nextLcsPair != null &&
                oldIndex === nextLcsPair[0] &&
                newIndex === nextLcsPair[1]
              ) {
                updatedDisplayArray.push(newDisplayArray[newIndex]) // no marker, since the value is "unchanged"
                oldIndex++
                newIndex++
                lcsIndex++
              } else {
                const oldInRange = oldIndex < oldDisplayArray.length
                const newInRange = newIndex < newDisplayArray.length
                const takeOld =
                  !newInRange || (oldInRange && (!nextLcsPair || oldIndex < nextLcsPair[0]))
                if (takeOld && oldInRange) {
                  const d = oldDisplayArray[oldIndex]
                  updatedDisplayArray.push({ text: d.text, abbr: d.abbr, marker: -1 })
                  oldIndex++
                } else if (newInRange) {
                  const d = newDisplayArray[newIndex]
                  updatedDisplayArray.push({ text: d.text, abbr: d.abbr, marker: 1 })
                  newIndex++
                }
              }
            }
            newDisplayArray = updatedDisplayArray
          }
        }
      }
      return {
        key,
        name,
        values: newDisplayArray,
      }
    })
    .filter((v) => v != null)
}

type EventPill = {
  flag: EventFlag | 'plus'
  text: string
}

function getPills(cell: Cell): EventPill[] {
  if (cell.events == null) {
    return []
  }
  let flags = [...cell.events.flags].filter(
    (f) => EVENT_FLAG_CATEGORIES[f] > EventCategory.MINISCULE,
  )
  const flagMap = new Map<EventCategory, EventFlag[]>()
  for (const flag of flags) {
    const category = EVENT_FLAG_CATEGORIES[flag]
    const array = flagMap.get(category) || []
    array.push(flag)
    if (array.length < 2) {
      flagMap.set(category, array)
    }
  }
  flags = []
  for (const category of [
    EventCategory.DELETED,
    EventCategory.ADDED,
    EventCategory.MAJOR,
    EventCategory.MINOR,
  ]) {
    const flagPart = flagMap.get(category) || []
    flagPart.reverse()
    flags.push(...flagPart)
  }
  const flagOverflow = flags.length > 4
  const flagPills = flags.slice(0, flagOverflow ? 3 : 4).map((flag) => {
    const text = EVENT_FLAG_PILLS[flag]
    return {
      flag,
      text,
    }
  })
  const bonusPills = flagOverflow ? [{ flag: 'plus' as const, text: `+${flags.length - 3}` }] : []
  return [...flagPills, ...bonusPills]
}

const UK_DATETIME_FORMATTER = new Intl.DateTimeFormat('en-GB', {
  day: 'numeric',
  month: 'numeric',
  year: 'numeric',
  hour: 'numeric',
  minute: 'numeric',
  timeZoneName: 'short',
})

const UK_DATE_FORMATTER = new Intl.DateTimeFormat('en-GB', {
  day: 'numeric',
  month: 'numeric',
  year: 'numeric',
})

// noinspection SpellCheckingInspection
function dateifyParam<T>(fn: (d: Date) => T): (d: string | Date) => T {
  return (date: string | Date) => {
    if (typeof date === 'string') {
      date = new Date(date)
    }
    return fn(date)
  }
}

const prettyDateTimeString = dateifyParam((date: Date) => {
  return UK_DATETIME_FORMATTER.format(date)
})

const prettyDateString = dateifyParam((date: Date) => {
  return UK_DATE_FORMATTER.format(normalizeDate(date))
})

function copyToClipboard(text: string): void {
  navigator.clipboard.writeText(text)
}
</script>

<template>
  <div id="wrapper" class="is-flex is-flex-direction-column is-align-items-stretch">
    <nav class="navbar is-flex-static" style="max-height: 52px">
      <div class="navbar-menu">
        <div class="navbar-start">
          <section class="buttons has-addons are-small w-elements-pane px-2 is-centered is-rounded">
            <button class="button is-link is-selected" @click="$router.push('search')">
              Search
            </button>
          </section>
        </div>
        <div
          class="navbar-item is-expanded is-flex is-justify-content-center is-align-items-center"
          v-if="searchResults.length > 0"
        >
          <button
            class="button is-rounded is-flex-static"
            style="width: 160px; border-radius: 16px 0 0 16px; justify-content: start"
            @click="previousTimes()"
            :disabled="+startDate <= +MIN_DATE"
          >
            <span class="icon">
              <i class="mdi mdi-arrow-left-thin"></i>
            </span>
            <span>Previous days</span>
          </button>
          <button
            class="button is-rounded is-flex-static"
            style="
              width: 160px;
              border-radius: 0 16px 16px 0;
              justify-content: end;
              margin-left: -0.75rem;
            "
            @click="nextTimes()"
            :disabled="+startDate >= +MAX_DATE"
          >
            <span>Next days</span>
            <span class="icon">
              <i class="mdi mdi-arrow-right-thin"></i>
            </span>
          </button>
          <button
            class="button is-rounded is-flex-static"
            style="width: 120px"
            @click="currentTimes()"
            :disabled="+startDate >= +MAX_DATE"
          >
            <span class="icon">
              <i class="mdi mdi-calendar-today"></i>
            </span>
            <span>Today</span>
          </button>
        </div>
      </div>
    </nav>
    <main class="is-flex-grow-1 is-flex is-align-items-stretch">
      <section
        class="elements-pane is-flex-static is-flex is-flex-direction-column is-align-items-stretch w-elements-pane"
      >
        <header
          class="is-flex-static h-header is-flex is-flex-direction-column is-align-items-stretch"
        >
          <section class="is-flex-grow-1 is-flex is-align-items-center p-3">
            <div class="field is-flex-grow-1">
              <div class="control has-icons-left has-icons-right">
                <input
                  class="input is-rounded"
                  type="search"
                  placeholder="Search..."
                  v-model="searchTerm"
                  @keydown="search()"
                  @change="search()"
                />
                <span class="icon is-left">
                  <i class="mdi mdi-magnify mdi-dark"></i>
                </span>
                <span class="icon is-right" @click="emptySearchTerm()">
                  <i class="mdi mdi-close mdi-dark"></i>
                </span>
              </div>
            </div>
          </section>
          <section class="is-flex-static buttons are-small pb-3 px-3 is-flex">
            <div class="is-flex-grow-1">
              <button class="button is-rounded" disabled>
                <span>Sort: Relevance</span>
                <span class="icon">
                  <i class="mdi mdi-chevron-right"></i>
                </span>
              </button>
            </div>
            <button class="button is-rounded is-flex-static" disabled>
              <span>Filters</span>
              <span class="icon">
                <i class="mdi mdi-chevron-right"></i>
              </span>
            </button>
            <button class="button is-rounded is-flex-static" disabled>
              <span>Time: Days</span>
              <span class="icon">
                <i class="mdi mdi-chevron-right"></i>
              </span>
            </button>
          </section>
        </header>
        <section class="is-flex-grow-1 is-position-relative is-overflow-y-scroll">
          <div
            v-for="element of elements"
            :key="element.result.id"
            class="element is-flex is-flex-direction-column is-align-items-stretch px-2 py-1"
            :class="{ deleted: element.deletedAt != null }"
          >
            <header class="is-flex-static">{{ element.header }}</header>
            <section class="name is-flex-grow-1 is-flex is-align-items-center">
              <span class="icon-text">
                <span
                  class="icon is-medium mr-1"
                  :class="{ 'shrink-icon': iconNeedsPadding(element.kind.icon) }"
                >
                  <img :src="`/icons/element/${element.kind.icon}.png`" />
                </span>
                <span>
                  <HighlightedText :text="element.name" :highlights="element.highlights" />
                  <span v-if="element.suffix" class="is-color-text-50">{{ element.suffix }}</span>
                </span>
              </span>
            </section>
            <footer class="is-flex-static is-flex is-align-items-center pb-1">
              <div
                v-for="chip in element.chips"
                :key="chip.username"
                class="chip"
                :class="{
                  'chip-very-high': chip.percentage >= 80,
                  'chip-high': 80 > chip.percentage && chip.percentage >= 60,
                  'chip-medium': 60 > chip.percentage && chip.percentage >= 40,
                  'chip-low': 40 > chip.percentage && chip.percentage >= 20,
                  'chip-very-low': chip.percentage < 20,
                }"
              >
                <img :src="`https://github.com/${chip.username}.png`" class="mr-1" />
                <span class="has-text-weight-semibold">{{ chip.percentage }}%</span>
              </div>
              <button
                class="button is-rounded is-small is-flex-static"
                style="box-shadow: none"
                :style="{ 'margin-left': element.chips.length < 2 ? '6rem' : '2rem' }"
                @click="elementTimes(element.createdAt)"
              >
                <span>Created on {{ prettyDateString(element.createdAt) }}</span>
                <span class="icon">
                  <i class="mdi mdi-chevron-right"></i>
                </span>
              </button>
            </footer>
          </div>
        </section>
        <footer
          class="is-flex-static is-flex is-flex-direction-column is-justify-content-center is-align-items-center p-3"
          v-if="searchResults.length > 0"
        >
          <button
            class="button is-rounded is-flex-static"
            style="width: 160px; border-radius: 16px 16px 0 0; justify-content: start"
            :disabled="startElementIndex <= 0"
            @click="previousElementPage()"
          >
            <span class="icon">
              <i class="mdi mdi-arrow-up-thin"></i>
            </span>
            <span>Previous results</span>
          </button>
          <button
            class="button is-rounded is-flex-static"
            style="width: 160px; border-radius: 0 0 16px 16px; justify-content: start"
            :disabled="startElementIndex >= searchResults.length - ELEMENT_SPAN"
            @click="nextElementPage()"
          >
            <span class="icon">
              <i class="mdi mdi-arrow-down-thin"></i>
            </span>
            <span>Next results</span>
          </button>
        </footer>
      </section>
      <section
        class="timeline-pane w-timeline is-flex-grow-1 is-flex is-flex-direction-column is-align-items-stretch"
      >
        <header
          class="timeline-header is-flex-static is-flex is-align-items-stretch h-header is-overflow-x-scroll"
        >
          <div
            class="column-header is-flex-static is-flex is-flex-direction-column is-justify-content-end is-align-items-center"
            v-for="date of dateObjects"
            :key="+date.date"
          >
            <div class="column-weekday">{{ date.weekday }}</div>
            <div class="column-month">{{ date.month }}</div>
            <div class="column-day is-size-3">{{ date.day }}</div>
          </div>
        </header>
        <section
          class="is-flex-grow-1 is-flex is-align-items-stretch is-overflow-x-scroll is-overflow-y-scroll"
        >
          <div
            v-for="(column, columnIndex) in cells"
            :key="columnHash(column, columnIndex)"
            class="timeline-column is-flex-static is-flex is-flex-direction-column is-align-items-stretch"
          >
            <div
              v-for="(cell, rowIndex) in column"
              :key="cellHash(cell, rowIndex, columnIndex)"
              class="timeline-cell is-flex-static"
            >
              <header v-if="cell.events != null" class="bar-pills">
                <div v-for="pill in getPills(cell)" :key="pill.flag">
                  {{ pill.text }}
                </div>
              </header>
              <div v-if="cell.last" class="bar-last"></div>
              <div v-if="!cell.starts" class="bar-start"></div>
              <div v-if="!cell.ends" class="bar-end"></div>
              <div
                v-if="cell.events != null"
                class="bar-spot"
                :class="{
                  'bar-spot-miniscule': cell.events.category === EventCategory.MINISCULE,
                }"
              >
                <AnPopover arrow class="bar-spot-popover" zIndex="9998">
                  <AnPopover
                    arrow
                    hover
                    openDelay="200"
                    closeDelay="100"
                    placement="top"
                    class="bar-spot-desc"
                    zIndex="9999"
                  >
                    <template #content>
                      <span
                        class="is-size-7 is-block"
                        style="margin-top: -4px; margin-bottom: -2px; white-space: nowrap"
                      >
                        {{ cell.events.hoverText }}
                      </span>
                    </template>
                    <button
                      class="button is-small is-rounded bar-event"
                      :class="{
                        'is-info': cell.events.category === EventCategory.MINOR,
                        'has-background-warning-45': cell.events.category === EventCategory.MAJOR,
                        'is-success': cell.events.category === EventCategory.ADDED,
                        'is-danger': cell.events.category === EventCategory.DELETED,
                      }"
                    >
                      <span
                        class="icon bright"
                        v-if="cell.events.category > EventCategory.MINISCULE"
                      >
                        <img :src="`/icons/event/${cell.events.mainFlag}.png`" />
                      </span>
                    </button>
                  </AnPopover>
                  <template #content>
                    <div class="bar-spot-popover-content">
                      <div class="is-size-7 mb-1" style="min-width: 100px; text-align: center">
                        {{ cell.events.list.length }} commit{{
                          cell.events.list.length > 1 ? 's' : ''
                        }}
                        with changes:
                      </div>
                      <table
                        class="table prop-table w-100 is-bordered is-striped is-hoverable is-narrow mb-0"
                      >
                        <tbody>
                          <template v-for="event of cell.events.list" :key="event.state.commit">
                            <tr class="is-selected">
                              <th
                                colspan="2"
                                @click="copyToClipboard(event.commit.hash)"
                                style="cursor: pointer"
                              >
                                <small class="is-size-7"
                                  >{{ event.commit.hash }} on
                                  {{ prettyDateTimeString(event.commit.date) }}</small
                                >
                                <ConditionalAbbr
                                  :title="
                                    event.commit.desc
                                      ? event.commit.summary + '\n\n' + event.commit.desc
                                      : null
                                  "
                                >
                                  {{ event.commit.summary }}
                                </ConditionalAbbr>
                              </th>
                            </tr>
                            <tr v-if="event.state.cause === ChangeCause.SUCCEEDED_PURE">
                              <td colspan="2" class="has-text-centered">
                                Branched or merged unchanged
                              </td>
                            </tr>
                            <tr v-if="event.state.cause === ChangeCause.SUCCEEDED_CHANGED">
                              <th class="prop-table-prop">Merged from</th>
                              <td
                                @click="copyToClipboard(event.sourceCommits[0].hash)"
                                style="cursor: pointer"
                              >
                                <ConditionalAbbr
                                  :title="
                                    event.sourceCommits[0].desc
                                      ? event.sourceCommits[0].summary +
                                        '\n\n' +
                                        event.sourceCommits[0].desc
                                      : null
                                  "
                                >
                                  {{ event.sourceCommits[0].hash }}
                                  {{ event.sourceCommits[0].summary }}
                                </ConditionalAbbr>
                              </td>
                            </tr>
                            <tr v-if="event.state.cause === ChangeCause.DELETED">
                              <td colspan="2" class="has-text-centered">
                                Symbol deleted with these properties:
                              </td>
                            </tr>
                            <template
                              v-for="prop of getDisplayProperties(event.state, rowIndex)"
                              :key="prop.key"
                            >
                              <tr v-for="(value, index) of prop.values" :key="index">
                                <th
                                  v-if="index === 0"
                                  class="prop-table-prop"
                                  :rowspan="prop.values.length"
                                >
                                  {{ prop.name }}
                                </th>
                                <td
                                  :class="{
                                    'prop-deleted-part': value.marker === -1,
                                    'prop-added-part': value.marker === 1,
                                  }"
                                >
                                  <ConditionalAbbr :title="value.abbr">
                                    {{ value.text }}
                                  </ConditionalAbbr>
                                </td>
                              </tr>
                            </template>
                          </template>
                        </tbody>
                      </table>
                    </div>
                  </template>
                </AnPopover>
                <div v-if="cell.events.list.length > 1" class="bar-plus">
                  +{{ cell.events.list.length - 1 }}
                </div>
              </div>
              <footer
                v-if="cell.events != null && cell.events.category > EventCategory.MINISCULE"
                class="is-flex is-align-items-center is-justify-content-center"
              >
                <div class="chips">
                  <template v-for="author in cell.events.authors" :key="author">
                    <AnPopover
                      arrow
                      hover
                      openDelay="200"
                      closeDelay="100"
                      placement="bottom"
                      zIndex="9998"
                    >
                      <template #content>
                        <span
                          class="is-size-7 is-block"
                          style="margin-top: -4px; margin-bottom: -2px"
                        >
                          {{ author }}
                        </span>
                      </template>
                      <img :src="`https://github.com/${author}.png`" />
                    </AnPopover>
                  </template>
                </div>
              </footer>
            </div>
          </div>
        </section>
      </section>
    </main>
  </div>
</template>

<style scoped lang="scss">
@use '@/assets/main' as *;
@use 'sass:color';

#wrapper {
  position: absolute;
  inset: 0;
  --popper-theme-background-color: #fafafa;
  --popper-theme-background-color-hover: #fafafa;
  --popper-theme-text-color: #222;
  --popper-theme-border-width: 1px;
  --popper-theme-border-style: solid;
  --popper-theme-border-radius: 6px;
  --popper-theme-border-color: #cacaca;
  --popper-theme-padding: 8px;
  --popper-theme-box-shadow: 0 6px 30px -6px rgba(0, 0, 0, 0.25);
}

main {
  height: calc(100% - 52px);
}

.w-timeline {
  width: calc(100% - $elements-pane-width);
}

.navbar {
  border-bottom: $navbar-border-width solid $primary-border-color;
}

.element {
  height: $cell-height;
  border-bottom: $secondary-border-width solid $secondary-border-color;
  overflow: hidden;

  &.deleted {
    .name {
      color: indianred;
    }

    :deep(.is-underlined) {
      text-decoration-style: wavy !important;
    }
  }

  > .name .icon + span {
    line-height: var(--bulma-icon-dimensions-medium);
    display: block;
    position: relative;
    top: -2px;
  }

  .shrink-icon {
    padding: 2px;
  }

  > header,
  > footer {
    font-size: small;
    padding-inline-start: calc(2rem + 0.5rem);
  }

  .chip {
    height: calc($result-profile-size + $result-profile-border-width * 2);
    border-top-right-radius: 9999px;
    border-bottom-right-radius: 9999px;
    display: flex;
    align-items: center;
    padding-inline-start: $result-profile-border-width;
    padding-inline-end: $result-profile-border-width * 1.41;

    + .chip {
      margin-left: 0.5rem;
    }

    img {
      height: $result-profile-size;
      width: $result-profile-size;
    }

    span {
      display: block;
      position: relative;
      top: -1.5px;
    }
  }
}

.elements-pane {
  border-right: $primary-border-width solid $primary-border-color;

  > header {
    border-bottom: $primary-border-width solid $primary-border-color;
  }
}

.button.is-rounded:has(.icon:only-child) {
  padding-inline-start: calc(var(--bulma-button-padding-horizontal) - 1px);
  padding-inline-end: calc(var(--bulma-button-padding-horizontal) - 1px);
}

.timeline-header {
  border-bottom: $primary-border-width solid $primary-border-color;
}

.column-header {
  width: $cell-width;
  border-right: $secondary-border-width solid $secondary-border-color;

  .column-weekday {
    margin-bottom: 8px;
  }

  .column-month {
    margin-bottom: -8px;
  }
}

.timeline-column {
  width: $cell-width;
  border-right: $secondary-border-width solid $secondary-border-color;
}

.timeline-cell {
  height: $cell-height;
  width: 100%;
  border-bottom: $secondary-border-width solid $secondary-border-color;
  position: relative;

  footer {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: calc(($cell-height - $timeline-spot-outline-diameter) / 2);

    .chips {
      padding: $timeline-profile-border-width;
      background-color: transparent;
      display: flex;
      justify-content: center;
      align-items: center;
      transition: 0.15s background-color;

      &:hover {
        background-color: lightskyblue;
      }

      img {
        display: block;
        width: $timeline-profile-size;
        height: $timeline-profile-size;
      }

      > div + div {
        margin-left: -12px + $timeline-profile-border-width !important;
      }
    }
  }
}

.bar-pills {
  $bar-column-gap: 2px;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: calc(($cell-height - $timeline-spot-outline-diameter) / 2);
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: center;
  flex-direction: row;
  column-gap: $bar-column-gap;
  row-gap: 2px;
  align-content: center;

  > div {
    font-size: 0.55rem;
    font-weight: bold;
    background-color: color.adjust(lightskyblue, $lightness: -40%);
    color: white;
    display: inline-block;
    width: calc($cell-width / 2 - 2 * $bar-column-gap);
    text-align: center;
    user-select: none;
    border-radius: 9999px;
    line-height: 10px;
    letter-spacing: 0.025rem;
    text-transform: uppercase;
    height: 11px;
  }
}

.bar-start,
.bar-end {
  $offset: calc($timeline-bar-width / 2);
  position: absolute;
  top: 50%;
  margin-top: -$offset;
  background-color: lightskyblue;
  height: $timeline-bar-width;
}

.bar-start {
  left: 0;
  width: 50%;
}

.bar-end {
  right: -$secondary-border-width;
  width: calc(50% + $secondary-border-width + 1px);
}

.bar-last {
  $height: $timeline-spot-outline-diameter;
  $width: 8px;
  $offset: calc($height / 2);
  position: absolute;
  top: 50%;
  margin-top: -$offset;
  background-color: lightskyblue;
  height: $height;
  width: $width;
  right: -$secondary-border-width;

  &::after {
    content: 'End';
    display: block;
    font-size: 0.75rem;
    font-weight: 600;
    z-index: 9997;
    position: absolute;
    top: 50%;
    margin-top: -0.6rem;
    left: 0.5rem;
    text-wrap: nowrap;
    color: #333;
  }
}

.bar-spot {
  --diameter: #{$timeline-spot-outline-diameter};
  background-color: lightskyblue;
  border-radius: calc(var(--diameter) / 2);
  position: absolute;
  top: 50%;
  left: 50%;
  width: var(--diameter);
  height: var(--diameter);
  margin-top: calc(var(--diameter) / -2);
  margin-left: calc(var(--diameter) / -2);
  display: flex;
  align-items: center;
  justify-content: center;

  &.bar-spot-miniscule {
    --diameter: 16px;

    button {
      margin-top: calc((#{$timeline-bar-width} - var(--diameter)) / 2);
      padding-inline: 0 !important;
      width: var(--diameter);
      height: var(--diameter);
    }

    .bar-plus {
      right: -1rem;
      bottom: -0.75rem;
    }
  }

  button {
    border: 1px solid #fafafa;
    padding-inline: var(--bulma-button-padding-horizontal) !important;
  }

  .bar-plus {
    position: absolute;
    right: -0.5rem;
    bottom: -0.25rem;
    font-size: 0.66rem;
    font-family: 'Tahoma', sans-serif;
    color: #fafafa;
    background-color: #444;
    border-radius: 4px;
    padding: 0 4px 2px;
    user-select: none;
  }

  .bar-spot-popover :deep(.popper) .bar-spot-popover-content {
    max-height: 300px;
    overflow-y: auto;
    padding: 0 14px 8px;
  }
}

.explore-path-name {
  display: block;
  position: relative;
  bottom: 1px;
}

.icon.bright {
  filter: brightness(255);
}

.nl-1 {
  position: relative;
  left: -1px;
}

$cell-hover-color: #f0f0f0;

.timeline-column:hover {
  background-color: $cell-hover-color;
}

@for $i from 1 through 100 {
  .elements-pane > section > .element:nth-child(#{$i}):hover {
    background-color: $cell-hover-color;

    main:has(&) > .timeline-pane .timeline-column > .timeline-cell:nth-child(#{$i}) {
      background-color: $cell-hover-color;
    }
  }

  main:has(.timeline-pane .timeline-column > .timeline-cell:nth-child(#{$i}):hover) {
    .elements-pane > section > .element:nth-child(#{$i}) {
      background-color: $cell-hover-color;
    }

    .timeline-pane .timeline-column > .timeline-cell:nth-child(#{$i}) {
      background-color: $cell-hover-color;
    }
  }
}

@for $i from 1 through 100 {
  .timeline-header > .column-header:nth-child(#{$i}):hover {
    background-color: $cell-hover-color;

    .timeline-pane:has(&) .timeline-column:nth-child(#{$i}) {
      background-color: $cell-hover-color;
    }
  }

  .timeline-pane:has(.timeline-column:nth-child(#{$i}):hover)
    .timeline-header
    > .column-header:nth-child(#{$i}) {
    background-color: $cell-hover-color;
  }
}

.prop-table {
  --prop-table-radius: 6px;
  border-collapse: separate;
  border-radius: var(--prop-table-radius);
  overflow: hidden;
  font-size: 0.9rem;

  tr:first-of-type :first-child {
    border-top-left-radius: var(--prop-table-radius);
  }

  tr:first-of-type :last-child {
    border-top-right-radius: var(--prop-table-radius);
  }

  tr:last-of-type :first-child {
    border-bottom-left-radius: var(--prop-table-radius);
  }

  tr:last-of-type :last-child {
    border-bottom-right-radius: var(--prop-table-radius);
  }

  tr.is-selected {
    th {
      text-align: center;
      text-wrap: nowrap;
    }

    small {
      display: block;
      margin-bottom: -4px;
    }
  }

  .prop-table-prop {
    text-align: right;
    font-weight: 400;
    text-wrap: nowrap;
    vertical-align: middle;

    + td {
      white-space: nowrap;
    }
  }

  .prop-added-part,
  .prop-deleted-part {
    position: relative;

    &::after {
      display: inline-block;
      color: white;
      font-weight: bold;
      width: 1rem;
      text-align: center;
      margin-left: 8px;
      height: 1rem;
      border-radius: 99px;
      line-height: 0.8rem;
      transform: translateY(-1px);
    }
  }

  .prop-added-part {
    background-color: color.adjust(lightgreen, $lightness: 20%);

    &::after {
      content: '+';
      background-color: green;
    }
  }

  .prop-deleted-part {
    background-color: color.adjust(lightcoral, $lightness: 25%);

    &::after {
      content: '-';
      background-color: darkred;
    }
  }
}

/* Temporary */

.navbar {
  flex-grow: 0 !important;
  flex-shrink: 0 !important;
  display: flex !important;
  align-items: stretch !important;
}

.navbar-menu {
  flex-grow: 1 !important;
  flex-shrink: 0 !important;
  display: flex !important;
  align-items: stretch !important;
}

.table tr.is-selected {
  background-color: color.adjust(hsl(171deg, 100%, 41%), $lightness: 30%, $saturation: -40%);
}
</style>
