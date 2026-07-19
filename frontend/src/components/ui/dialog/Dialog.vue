<script setup lang="ts">
import { ref, onMounted } from "vue"
import type { DialogRootEmits, DialogRootProps } from "reka-ui"
import { DialogRoot, useForwardPropsEmits } from "reka-ui"

const props = defineProps<DialogRootProps>()
const emits = defineEmits<DialogRootEmits>()

const forwarded = useForwardPropsEmits(props, emits)
const isClient = ref(false)

onMounted(() => {
  isClient.value = true
})
</script>

<template>
  <DialogRoot v-if="isClient" v-bind="forwarded">
    <slot />
  </DialogRoot>
  <template v-else>
    <slot />
  </template>
</template>
