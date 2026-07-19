
<script setup lang="ts">
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'
import type { CalendarEventModel } from '@/data/reminder'
import { ReminderType } from '@/data/reminder'

interface Props {
  event: CalendarEventModel
  caseName: string
}

defineProps<Props>()

const getReminderTypeLabel = (type: ReminderType) => {
  return type === ReminderType.KeyDate ? '关键日期' : '自定义事项'
}

const getReminderTypeVariant = (type: ReminderType) => {
  return type === ReminderType.KeyDate ? 'destructive' : 'default'
}

const formatTime = (dateStr: string) => {
  const date = new Date(dateStr)
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}
</script>

<template>
  <div class="border rounded-lg p-3 space-y-2 hover:bg-accent transition-colors">
    <div class="flex items-start justify-between gap-2">
      <div class="flex-1">
        <p class="font-medium text-sm line-clamp-2">{{ event.title }}</p>
        <p class="text-xs text-muted-foreground mt-1">{{ caseName }}</p>
      </div>
      <Badge :variant="getReminderTypeVariant(event.reminderType)" class="shrink-0">
        {{ getReminderTypeLabel(event.reminderType) }}
      </Badge>
    </div>

    <div class="flex items-center gap-2 text-xs text-muted-foreground">
      <SafeIcon name="Clock" :size="14" />
      <span>{{ formatTime(event.start) }}</span>
    </div>

    <Button
      variant="outline"
      size="sm"
      class="w-full"
      as="a"
      :href="`./case-detail-view.html?id=${event.caseId}`"
    >
      <SafeIcon name="Eye" :size="14" class="mr-1" />
      查看案件详情
    </Button>
  </div>
</template>
