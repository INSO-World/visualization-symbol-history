<script setup lang="ts">
import Popper from 'vue3-popper'
</script>

<template>
<Popper v-bind="$attrs">
  <slot />
  <template #content="props">
    <slot name="content" v-bind="props" />
  </template>
</Popper>
</template>

<style scoped lang="scss">

</style>
