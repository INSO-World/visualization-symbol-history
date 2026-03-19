<script setup lang="ts">
import type { SymbolElement } from '@/models/SymbolElement'
import { computed } from 'vue'

const { text = '', highlights = [] } = defineProps<{
  text: string
  highlights: SymbolElement['highlights']
}>()

const segments = computed(() => {
  const result: Array<{ range: [number, number]; highlighted: boolean }> = []
  let lastIndex: number = 0
  for (const highlight of highlights) {
    if (lastIndex < highlight[0]) {
      result.push({ range: [lastIndex, highlight[0]], highlighted: false })
    }
    result.push({ range: [highlight[0], highlight[1]], highlighted: true })
    lastIndex = highlight[1]
  }
  if (lastIndex < text.length) {
    result.push({ range: [lastIndex, text.length], highlighted: false })
  }
  return result.map((entry) => ({
    txt: text.substring(...entry.range),
    highlighted: entry.highlighted,
  }))
})
</script>

<template>
  <b class="has-text-weight-semibold">
    <span
      v-for="(segment, index) of segments"
      :key="index"
      :class="{ 'has-text-weight-bold is-underlined': segment.highlighted }"
    >
      {{ segment.txt }}
    </span>
  </b>
</template>

<style scoped lang="scss"></style>
