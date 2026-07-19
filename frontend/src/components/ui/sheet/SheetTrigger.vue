<script setup lang="ts">
import type { DialogTriggerProps } from "reka-ui"
import { DialogTrigger } from "reka-ui"
import { ref, onMounted } from "vue"

const props = defineProps<DialogTriggerProps>()
const isClient = ref(false)

onMounted(() => {
  isClient.value = true
})
</script>

<template>
  <DialogTrigger v-if="isClient" v-bind="props">
    <slot />
  </DialogTrigger>
  <template v-else>
    <slot />
  </template>
</template>
