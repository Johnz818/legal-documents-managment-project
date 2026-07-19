<script setup lang="ts">
import type { SelectTriggerProps } from "reka-ui"
import type { HTMLAttributes } from "vue"
import { ref, onMounted } from "vue"
import { reactiveOmit } from "@vueuse/core"
import { ChevronDown } from "lucide-vue-next"
import { SelectIcon, SelectTrigger, useForwardProps } from "reka-ui"
import { cn } from "@/lib/utils"

const props = defineProps<SelectTriggerProps & { class?: HTMLAttributes["class"] }>()
const isClient = ref(false)

const delegatedProps = reactiveOmit(props, "class")

const forwardedProps = useForwardProps(delegatedProps)

onMounted(() => {
  isClient.value = true
})
</script>

<template>
  <SelectTrigger
    v-if="isClient"
    v-bind="forwardedProps"
    :class="cn(
      'flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background data-[placeholder]:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 [&>span]:truncate text-start',
      props.class,
    )"
  >
    <slot />
    <SelectIcon as-child>
      <ChevronDown class="w-4 h-4 opacity-50 shrink-0" />
    </SelectIcon>
  </SelectTrigger>
  <div v-else :class="cn(
    'flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background',
    props.class,
  )">
    <slot />
    <ChevronDown class="w-4 h-4 opacity-50 shrink-0" />
  </div>
</template>
