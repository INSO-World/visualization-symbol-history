<!--suppress HtmlRequiredAltAttribute -->
<script setup lang="ts">
import { computed, ref } from 'vue'
import { type Cell, EventFlag } from '@/models/Cell'
import type { SymbolEvent } from '@/models/SymbolEvent'
import { addDays } from '@/models/mocks'
import { toDateObject } from '@/models/DateObject'
import type { SymbolElement } from '@/models/SymbolElement'
import {
  ChangeCause,
  type ConcreteTypeArgument,
  Kind,
  Modifier,
  type StateDto,
  type StateField,
  type TypeArgument,
  type TypeBound,
  type UnknownType,
  UpdateFlag,
  type UpperTypeBound,
} from '@/models/analyzer'
import { type SearchResult, useAnalyzerStore } from '@/stores/analyzer'
import HighlightedText from '@/components/HighlightedText.vue'
import debounce from 'debounce'

const analyzerStore = useAnalyzerStore()

const searchTerm = ref<string>('')

const DATE_SPAN = 18
const TYPE_CHARACTER_LIMIT = 24
const PATH_CHARACTER_LIMIT = 32
const MIN_DATE = new Date("2020-01-01Z")
const TODAY = new Date(new Date().toISOString().substring(0, 10))
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
const cells = ref([] as Cell[][])

type KindInfo = {
  icon: string
  abstract: boolean
  typed: boolean
  parameterized: boolean
  text: string
}

const KIND_MAPPING: Record<Kind, KindInfo> = {
  ANNOTATION: {
    icon: 'annotation',
    abstract: false,
    typed: false,
    parameterized: false,
    text: 'annotation',
  },
  CLASS: {
    icon: 'class',
    abstract: true,
    typed: false,
    parameterized: false,
    text: 'class',
  },
  CONSTANT_FIELD: {
    icon: 'constant',
    abstract: false,
    typed: true,
    parameterized: false,
    text: 'class constant',
  },
  CONSTANT_VARIABLE: {
    icon: 'constant',
    abstract: false,
    typed: true,
    parameterized: false,
    text: 'local constant',
  },
  CONSTRUCTOR: {
    icon: 'constructor',
    abstract: false,
    typed: false,
    parameterized: true,
    text: 'constructor',
  },
  ENUM: {
    icon: 'enum',
    abstract: false,
    typed: false,
    parameterized: false,
    text: 'enum',
  },
  ENUM_CONSTANT: {
    icon: 'enum_constant',
    abstract: false,
    typed: false,
    parameterized: false,
    text: 'enum constant',
  },
  FIELD: {
    icon: 'field',
    abstract: false,
    typed: true,
    parameterized: false,
    text: 'field',
  },
  INTERFACE: {
    icon: 'interface',
    abstract: false,
    typed: false,
    parameterized: false,
    text: 'interface',
  },
  METHOD: {
    icon: 'method',
    abstract: true,
    typed: true,
    parameterized: true,
    text: 'method',
  },
  MODULE: {
    icon: 'module',
    abstract: false,
    typed: false,
    parameterized: false,
    text: 'module',
  },
  PACKAGE: {
    icon: 'package',
    abstract: false,
    typed: false,
    parameterized: false,
    text: 'package',
  },
  PARAMETER: {
    icon: 'parameter',
    abstract: false,
    typed: true,
    parameterized: false,
    text: 'parameter',
  },
  RECORD: {
    icon: 'record',
    abstract: false,
    typed: false,
    parameterized: false,
    text: 'record class',
  },
  VARIABLE: {
    icon: 'variable',
    abstract: false,
    typed: true,
    parameterized: false,
    text: 'variable',
  },
}

function capitalize(s: string): string {
  return s.charAt(0).toUpperCase() + s.slice(1)
}

function isConcreteTypeArg(a: TypeArgument): a is ConcreteTypeArgument {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return (a as any)['type'] != null
}

function isUpperBound(b: TypeBound): b is UpperTypeBound {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return (b as any)['extends'] != null
}

function typeToText(t: UnknownType, deep: boolean = true): string {
  let result = t.qualifiedName.replace(/\$/g, '.').split('.').at(-1)!
  if (deep && t.typeArguments != null) {
    const parts = t.typeArguments.map((arg) => {
      if (isConcreteTypeArg(arg)) {
        return typeToText(arg.type, false)
      } else if (arg.wildcard === 'any') {
        return '?'
      } else if (isUpperBound(arg.wildcard)) {
        return `? extends ${typeToText(arg.wildcard.extends, false)}`
      } else {
        return `? super ${typeToText(arg.wildcard.super, false)}`
      }
    })
    result += '<'
    for (let i = 0; i < parts.length; i++) {
      if (i > 0) {
        result += ', '
      }
      const part = parts[i]
      if (result.length + part.length + 2 > TYPE_CHARACTER_LIMIT) {
        result += '…'
        break
      }
      result += part
    }
    result += '>'
  }
  return result
}


function resultToElement(result: SearchResult): SymbolElement {
  const kindInfo = KIND_MAPPING[result.key.kind]
  let headerText = kindInfo.text
  let iconName = kindInfo.icon
  let suffixText = ''
  const keyState = analyzerStore.findKeyState(result.symbol, result.key)
  if (kindInfo.abstract) {
    const abstract = new Set(keyState.properties['modifiers']).has(Modifier.ABSTRACT)
    if (abstract) {
      headerText = 'abstract ' + headerText
      iconName += '_abstract'
    }
  }
  if (kindInfo.parameterized) {
    suffixText += '(…)'
  }
  if (kindInfo.typed) {
    const type = keyState.properties['type']
    if (type != null) {
      suffixText += `: ${typeToText(type)}`
    }
  }
  headerText = capitalize(headerText)
  const parentCrumbs = analyzerStore
    .findParentCrumbs(result.key)
    .slice(0, -1)
    .map((k) => k.name)
  let parentText = ''
  for (let i = parentCrumbs.length - 1; i >= 0; i--) {
    const crumb = parentCrumbs[i]
    if (parentText.length + crumb.length + 1 > PATH_CHARACTER_LIMIT) {
      parentText = '…' + parentText
      break
    }
    parentText = `${crumb}${parentText.length === 0 ? '' : '.'}${parentText}`
  }
  headerText += ` in ${parentText}`
  return {
    result: result.symbol,
    header: headerText,
    icon: iconName,
    name: result.key.name,
    suffix: suffixText,
    highlights: result.match,
    chips: [
      {
        username: 'AM307',
        percentage: 100,
      },
    ],
    score: result.score,
    deleted: result.symbol.deleted,
  }
}

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
    .map(resultToElement)
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

const UPDATE_FLAG_MAPPING: Record<UpdateFlag, EventFlag | undefined> = {
  BODY_UPDATED: EventFlag.BODY,
  MOVED: EventFlag.MOVED,
  MOVED_WITH_PARENT: undefined,
  RENAMED: EventFlag.RENAMED,
  REORDERED: EventFlag.REORDERED,
  REPLACED: EventFlag.REPLACED,
}

function getEventFlags(state: StateDto): Set<EventFlag> {
  const result = new Set<EventFlag>()
  if (state.cause === ChangeCause.ADDED) {
    result.add(EventFlag.ADDED)
  } else if (state.cause === ChangeCause.DELETED) {
    result.add(EventFlag.DELETED)
  } else if (state.cause === ChangeCause.CHANGED) {
    // Do nothing
  } else {
    result.add(EventFlag.BRANCHED)
  }
  if (state.flags != null) {
    const eventFlags = new Set<EventFlag>(
      state.flags
      .map((f) => UPDATE_FLAG_MAPPING[f])
      .filter((f) => f != null)
    )
    if (state.flags.includes(UpdateFlag.MOVED_WITH_PARENT)) {
      eventFlags.delete(EventFlag.MOVED)
    }
    eventFlags.forEach(f => result.add(f))
  }
  const updatedSet = new Set<StateField>(state.updated)
  if (updatedSet.has('initialValue') || updatedSet.has('enumArguments')) {
    result.add(EventFlag.VALUE)
  }
  if (updatedSet.has('annotations')) {
    result.add(EventFlag.ANNOTATIONS)
  }
  if (updatedSet.has('kind')) {
    result.add(EventFlag.KIND)
  }
  if (updatedSet.has('modifiers')) {
    result.add(EventFlag.MODIFIERS)
  }
  if (updatedSet.has('realizations')) {
    result.add(EventFlag.REALIZATIONS)
  }
  if (updatedSet.has('supertypes')) {
    result.add(EventFlag.SUPERTYPES)
  }
  if (updatedSet.has('typeParameters')) {
    result.add(EventFlag.TYPE_PARAMETERS)
  }
  if (updatedSet.has('type')) {
    result.add(EventFlag.TYPE)
  }
  if (updatedSet.has('visibility')) {
    result.add(EventFlag.VISIBILITY)
  }
  return result
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
          date: analyzerStore.commitDate(state.commit),
          authors: ['AM307'],
        })),
      )
    }
    newSymbolEvents.push(events)
  }
  console.log(newSymbolEvents)
  symbolEvents.value = newSymbolEvents

  const started: boolean[] = symbolEvents.value.map((_a, i) => {
    const result = elements.value[i].result
    if (result.deleted) {
      const deletedStates = Object.values(result.states).flatMap(s => s).filter(s => s.cause === ChangeCause.DELETED)
      if (deletedStates.length > 0 && analyzerStore.commitDate(deletedStates[0].commit).valueOf() < startDate.value.valueOf()) {
        return false
      }
    }
    const added = new Date(result.keys[0].from)
    return +added < startDate.value.valueOf()
  })

  const eventsToProcess = [
    ...Map.groupBy(
      symbolEvents.value.flatMap((a, i) => a.map((e) => ({ index: i, ...e }))),
      (e: SymbolEvent & { index: number }) =>
        new Date(e.date.toISOString().substring(0, 10)).valueOf(),
    ),
  ].map(([date, entries]) => ({ date, entries }))
  eventsToProcess.sort((a, b) => a.date - b.date)
  while (eventsToProcess.length > 0 && +eventsToProcess[0].date < +startDate.value) {
    eventsToProcess.shift()
  }
  const newCells: Cell[][] = []
  for (let i = 0; i < dateObjects.value.length; i++) {
    const column: Cell[] = []
    const { date } = dateObjects.value[i]
    const dateEvents = []
    if (eventsToProcess.length > 0 && +eventsToProcess[0].date === +date) {
      dateEvents.push(...eventsToProcess.shift()!.entries)
    }
    for (let el = 0; el < symbolEvents.value.length; el++) {
      const event = dateEvents.find((e) => e.index === el) // TODO: Collect ALL events
      if (event != null) {
        const cell: Cell = {
          event: { state: event.state, flags: getEventFlags(event.state), authors: event.authors },
          starts: false,
          ends: false,
        }
        if (cell.event!.flags.size === 0) {
          const running = started[el]
          column.push({ starts: !running, ends: !running })
          continue
        }
        if (cell.event!.state.cause === ChangeCause.ADDED) {
          started[el] = true
          cell.starts = true
        }
        if (cell.event!.state.cause === ChangeCause.DELETED) {
          started[el] = false
          cell.ends = true
        }
        column.push(cell)
      } else {
        const running = started[el]
        column.push({ starts: !running, ends: !running })
      }
    }
    newCells.push(column)
  }
  cells.value = newCells
}

function iconNeedsPadding(icon: string): boolean {
  return ['field_injected', 'field', 'constant'].includes(icon)
}
</script>

<template>
  <div id="wrapper" class="is-flex is-flex-direction-column is-align-items-stretch">
    <nav class="navbar is-flex-static">
      <div class="navbar-menu">
        <div class="navbar-start">
          <section class="buttons has-addons are-small w-elements-pane px-2 is-centered is-rounded">
            <button class="button is-link is-selected" @click="$router.push('search')">
              Search
            </button>
          </section>
        </div>
        <div class="navbar-item is-expanded is-flex is-justify-content-center is-align-items-center pr-6 pl-0" v-if="searchResults.length > 0">
          <button
            class="button is-rounded is-flex-static"
            style="width: 160px; border-radius: 16px 0 0 16px; justify-content: start"
            @click="previousTimes()"
          >
            <span class="icon">
              <i class="mdi mdi-chevron-left"></i>
            </span>
            <span>Previous days</span>
          </button>
          <button
            class="button is-flex-static"
            style="width: 120px; border-radius: 0"
            @click="currentTimes()"
          >
            <span>Today</span>
          </button>
          <button
            class="button is-rounded is-flex-static"
            style="width: 160px; border-radius: 0 16px 16px 0; justify-content: end"
            @click="nextTimes()"
          >
            <span>Next days</span>
            <span class="icon">
              <i class="mdi mdi-chevron-right"></i>
            </span>
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
              <button class="button is-rounded">
                <span>Sort: Relevance</span>
                <span class="icon">
                  <i class="mdi mdi-chevron-right"></i>
                </span>
              </button>
            </div>
            <button class="button is-rounded is-flex-static">
              <span>Filters</span>
              <span class="icon">
                <i class="mdi mdi-chevron-right"></i>
              </span>
            </button>
            <button class="button is-rounded is-flex-static">
              <span>Time: Days</span>
              <!--<span class="icon">
                <i class="mdi mdi-chevron-right"></i>
              </span>-->
            </button>
          </section>
        </header>
        <section class="is-flex-grow-1 is-position-relative is-overflow-y-scroll">
          <div
            v-for="element of elements"
            :key="element.result.id"
            class="element is-flex is-flex-direction-column is-align-items-stretch px-2 py-1"
            :class="{ deleted: element.deleted }"
          >
            <header class="is-flex-static">{{ element.header }}</header>
            <section class="name is-flex-grow-1 is-flex is-align-items-center">
              <span class="icon-text">
                <span
                  class="icon is-medium mr-1"
                  :class="{ 'shrink-icon': iconNeedsPadding(element.icon) }"
                >
                  <img :src="`/icons/element/${element.icon}.png`" />
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
                style="box-shadow: none; margin-left: 8rem"
              >
                <span>Jump to Created</span>
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
              <i class="mdi mdi-chevron-up"></i>
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
              <i class="mdi mdi-chevron-down"></i>
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
              v-for="(cell, cellIndex) in column"
              :key="cellIndex"
              class="timeline-cell is-flex-static"
            >
              <div v-if="!cell.starts" class="bar-start"></div>
              <div v-if="!cell.ends" class="bar-end"></div>
              <div v-if="cell.event" class="bar-spot">
                <button
                  class="button is-small is-rounded bar-event"
                  :class="{
                    'is-success': cell.event.flags.has(EventFlag.ADDED),
                    'is-danger': cell.event.flags.has(EventFlag.DELETED),
                    'is-info': [
                      EventFlag.BRANCHED,
                      EventFlag.VALUE,
                      EventFlag.REORDERED,
                      EventFlag.BODY,
                    ].some(f => cell.event!.flags.has(f)),
                    'has-background-warning-45': [
                      EventFlag.REPLACED,
                      EventFlag.ANNOTATIONS,
                      EventFlag.KIND,
                      EventFlag.MOVED,
                      EventFlag.RENAMED,
                      EventFlag.MODIFIERS,
                      EventFlag.REALIZATIONS,
                      EventFlag.SUPERTYPES,
                      EventFlag.TYPE_PARAMETERS,
                      EventFlag.TYPE,
                      EventFlag.VISIBILITY,
                    ].some(f => cell.event!.flags.has(f)),
                  }"
                >
                  <span class="icon bright">
                    <img :src="`/icons/event/${cell.event.flags.values().next().value}.png`" />
                  </span>
                </button>
              </div>
              <footer
                v-if="cell.event"
                class="is-flex is-align-items-center is-justify-content-center"
              >
                <div class="chips">
                  <img
                    v-for="(author, authorIndex) in cell.event.authors"
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
  $radius: calc($timeline-spot-outline-diameter / 2);
  background-color: lightskyblue;
  border-radius: $radius;
  position: absolute;
  top: 50%;
  left: 50%;
  width: $timeline-spot-outline-diameter;
  height: $timeline-spot-outline-diameter;
  margin-top: -$radius;
  margin-left: -$radius;
  display: flex;
  align-items: center;
  justify-content: center;

  button {
    border: 1px solid #fafafa;
    padding-inline: var(--bulma-button-padding-horizontal) !important;
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
