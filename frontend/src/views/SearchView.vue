<!--suppress HtmlRequiredAltAttribute -->
<script setup lang="ts">
import { computed, ref, shallowRef, triggerRef } from 'vue'
import { type Cell, type CellEvent, CellEventCategory, EventFlag } from '@/models/Cell'
import type { SymbolEvent } from '@/models/SymbolEvent'
import { toDateObject } from '@/models/DateObject'
import type { SymbolElement } from '@/models/SymbolElement'
import { ChangeCause, Kind, type PropertyKey, type StateDto } from '@/models/analyzer'
import { useAnalyzerStore } from '@/stores/analyzer'
import HighlightedText from '@/components/HighlightedText.vue'
import debounce from 'debounce'
import { addDays, normalizeDate } from '@/functions/date'
import { union } from '@/functions/lang'
import { resultToElement } from '@/functions/element'
import { getCellEventCategory, getEventFlags } from '@/functions/event-flags'
import AnPopover from '@/components/AnPopover.vue'
import {
  type DisplayPropertyKey,
  PROPERTIES_TO_DISPLAY,
  PROPERTY_DISPLAY_NAMES,
} from '@/constants/property-display-names'
import { PROPERTY_DISPLAYS } from '@/functions/displays'

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
const symbolEvents = ref([] as SymbolEvent[][])
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
  const newSymbolEvents: SymbolEvent[][] = []
  for (const element of elements.value) {
    const events: SymbolEvent[] = []
    for (const yearMonth of yearMonths.value) {
      const ymEvents: StateDto[] | undefined = element.result.states[yearMonth]
      if (ymEvents == null || ymEvents.length === 0) {
        continue
      }
      events.push(
        ...ymEvents.map((state) => ({
          state,
          date: normalizeDate(analyzerStore.commitDate(state.commit)),
          authors: ['AM307'],
        })),
      )
    }
    newSymbolEvents.push(events)
  }
  //console.log(newSymbolEvents)
  symbolEvents.value = newSymbolEvents

  const started: boolean[] = symbolEvents.value.map((_a, i) => {
    const element = elements.value[i]
    if (element.deletedAt != null && +element.deletedAt < +startDate.value) {
      return false
    }
    return +element.createdAt < +startDate.value
  })

  const eventsToProcess = [
    ...Map.groupBy(
      symbolEvents.value.flatMap((a, i) => a.map((e) => ({ index: i, ...e }))),
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
    for (let e = 0; e < symbolEvents.value.length; e++) {
      const events = dateEvents.filter((ev) => ev.index === e)
      if (events.length > 0) {
        events.sort((a, b) => +a.date - +b.date)
        const list: CellEvent[] = events.map((ev) => {
          const flags = getEventFlags(ev.state)
          const category = Math.max(
            CellEventCategory.MINISCULE,
            ...[...flags].map((f) => getCellEventCategory(f)),
          )
          return {
            category,
            state: ev.state,
            commit: analyzerStore.getCommit(ev.state.commit),
            flags,
            authors: ev.authors,
          }
        })
        const mainCategory = Math.max(...list.map((ev) => ev.category))
        const allFlags = union(list.map((ev) => ev.flags))
        const mainFlagCandidates: EventFlag[] = list
          .flatMap((ev) => [...ev.flags])
          .filter((f) => getCellEventCategory(f) === mainCategory)
        const cell: Cell = {
          events: {
            list,
            category: mainCategory,
            flags: allFlags,
            mainFlag: mainFlagCandidates.at(-1)!,
            authors: [...new Set(list.flatMap((ev) => ev.authors))],
          },
          starts: false,
          ends: false,
        }
        /*if (cell.events!.category === CellEventCategory.MINISCULE) {
          const running = started[e]
          column.push({ starts: !running, ends: !running })
          continue
        }*/
        if (cell.events!.flags.has(EventFlag.ADDED)) {
          started[e] = true
          cell.starts = true
        }
        if (cell.events!.flags.has(EventFlag.DELETED)) {
          started[e] = false
          cell.ends = true
        }
        column.push(cell)
      } else {
        const running = started[e]
        column.push({ starts: !running, ends: !running })
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

function columnHash(cells: Cell[]): string {
  return cells
    .map((cell) => {
      if (cell.events == null) {
        return '='
      }
      return cell.events.list.map((e) => `${e.state.symbolId}@${e.state.commit}`)
    })
    .join(' ')
}

function getDisplayProperties(
  state: StateDto,
): Array<{ key: DisplayPropertyKey; name: string; values: string[] }> {
  const updatedKeys: PropertyKey[] =
    state.cause === ChangeCause.ADDED
      ? (Object.keys(state.properties).filter((k) => k !== 'body') as PropertyKey[])
      : (state.updated ?? [])
  const keys = updatedKeys.filter((key) => PROPERTIES_TO_DISPLAY.has(key)) as DisplayPropertyKey[]
  return keys.map((key) => {
    let name = PROPERTY_DISPLAY_NAMES[key]
    if (key === 'type' && state.properties.kind === Kind.METHOD) {
      name = 'Return ' + name.toLowerCase()
    }
    return {
      key,
      name,
      values: PROPERTY_DISPLAYS[key](state.properties[key]! as never),
    }
  })
}

const UK_DATE_FORMATTER = new Intl.DateTimeFormat('en-GB', {
  day: 'numeric',
  month: 'numeric',
  year: 'numeric',
  hour: 'numeric',
  minute: 'numeric',
  timeZoneName: 'short',
})

function prettyDateString(dateString: string): string {
  return UK_DATE_FORMATTER.format(new Date(dateString))
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
                style="box-shadow: none; margin-left: 6rem"
                @click="elementTimes(element.createdAt)"
              >
                <span>Created on {{ element.createdAt.toISOString().substring(0, 10) }}</span>
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
            :key="columnIndex"
            class="timeline-column is-flex-static is-flex is-flex-direction-column is-align-items-stretch"
          >
            <div
              v-for="(cell, rowIndex) in column"
              :key="rowIndex"
              class="timeline-cell is-flex-static"
            >
              <div v-if="!cell.starts" class="bar-start"></div>
              <div v-if="!cell.ends" class="bar-end"></div>
              <div
                v-if="cell.events != null"
                class="bar-spot"
                :class="{
                  'bar-spot-miniscule': cell.events.category === CellEventCategory.MINISCULE,
                }"
              >
                <AnPopover arrow>
                  <button
                    class="button is-small is-rounded bar-event"
                    :class="{
                      'is-info': cell.events.category === CellEventCategory.MINOR,
                      'has-background-warning-45': cell.events.category === CellEventCategory.MAJOR,
                      'is-success': cell.events.category === CellEventCategory.ADDED,
                      'is-danger': cell.events.category === CellEventCategory.DELETED,
                    }"
                  >
                    <span
                      class="icon bright"
                      v-if="cell.events.category > CellEventCategory.MINISCULE"
                    >
                      <img :src="`/icons/event/${cell.events.mainFlag}.png`" />
                    </span>
                  </button>
                  <template #content>
                    <div class="is-size-7 mb-1" style="min-width: 100px; text-align: center">
                      {{ cell.events.list.length }} change{{
                        cell.events.list.length > 1 ? 's:' : ':'
                      }}
                    </div>
                    <table
                      class="table prop-table w-100 is-bordered is-striped is-hoverable is-narrow mb-0"
                    >
                      <tbody>
                        <template v-for="event of cell.events.list" :key="event.state.commit">
                          <tr class="is-selected">
                            <th colspan="2">
                              <small class="is-size-7"
                                >{{ event.commit.hash }} on
                                {{ prettyDateString(event.commit.date) }}</small
                              >
                              <abbr
                                :title="
                                  event.commit.summary +
                                  (event.commit.desc ? '\n\n' + event.commit.desc : '')
                                "
                              >
                                {{ event.commit.summary }}
                              </abbr>
                            </th>
                          </tr>
                          <tr v-if="event.state.cause.startsWith('SUCCEEDED')">
                            <td colspan="2" class="has-text-centered">Branched or merged</td>
                          </tr>
                          <tr v-if="event.state.cause === ChangeCause.DELETED">
                            <td colspan="2" class="has-text-centered">Symbol deleted</td>
                          </tr>
                          <template
                            v-for="prop of getDisplayProperties(event.state)"
                            :key="prop.key"
                          >
                            <tr v-for="(value, index) of prop.values" :key="index">
                              <th
                                v-if="index === 0"
                                class="prop-table-prop has-text-right has-text-weight-normal"
                                :rowspan="prop.values.length"
                              >
                                {{ prop.name }}
                              </th>
                              <td>{{ value }}</td>
                            </tr>
                          </template>
                        </template>
                      </tbody>
                    </table>
                  </template>
                </AnPopover>
                <div v-if="cell.events.list.length > 1" class="bar-plus">
                  +{{ cell.events.list.length - 1 }}
                </div>
              </div>
              <footer
                v-if="cell.events != null && cell.events.category > CellEventCategory.MINISCULE"
                class="is-flex is-align-items-center is-justify-content-center"
              >
                <div class="chips">
                  <img
                    v-for="(author, authorIndex) in cell.events.authors"
                    :key="authorIndex"
                    :src="`https://github.com/${author}.png`"
                  />
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

        + img {
          margin-left: $timeline-profile-border-width;
        }
      }
    }
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
    text-wrap: nowrap;
    vertical-align: middle;
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
</style>
