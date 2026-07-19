<script setup lang="ts">
import type { PopoverTriggerProps } from "reka-ui"
import { PopoverTrigger } from "reka-ui"
import { ref, onMounted } from "vue"

const props = defineProps<PopoverTriggerProps>()
const isClient = ref(false)

onMounted(() => {
  isClient.value = true
})
</script>

<template>
  <PopoverTrigger v-if="isClient" v-bind="props">
    <slot />
  </PopoverTrigger>
  <template v-else>
    <slot />
  </template>
</template>
