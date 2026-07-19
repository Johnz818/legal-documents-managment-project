
<script setup lang="ts">
import { Button } from '@/components/ui/button'
import SafeIcon from '@/components/common/SafeIcon.vue'

interface Props {
  monthYear: string
}

defineProps<Props>()

const emit = defineEmits<{
  previousMonth: []
  nextMonth: []
}>()
</script>

<template>
  <div class="flex items-center justify-between">
    <h2 class="text-lg font-semibold">{{ monthYear }}</h2>
    <div class="flex gap-2">
      <Button variant="outline" size="icon" @click="emit('previousMonth')">
        <SafeIcon name="ChevronLeft" :size="20" />
      </Button>
      <Button variant="outline" size="icon" @click="emit('nextMonth')">
        <SafeIcon name="ChevronRight" :size="20" />
      </Button>
    </div>
  </div>
</template>
