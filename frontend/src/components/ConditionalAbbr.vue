<script setup lang="ts">
const { title } = defineProps<{
  title: string | null | undefined
}>()
</script>

<template>
  <template v-if="title == null">
    <slot />
  </template>
  <abbr v-else :title="title">
    <slot />
  </abbr>
</template>

<style scoped lang="scss">

</style>
