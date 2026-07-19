
<script setup lang="ts">
import type { ReminderModel, ReminderType } from '@/data/reminder'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'
import EmptyState from '@/components/common/EmptyState.vue'

interface Props {
  reminders: ReminderModel[]
  getCaseName: (caseId: string) => string
  getReminderTypeColor: (type: ReminderType) => string
  formatDateTime: (dateStr: string) => string
  isOverdue: (targetDate: string) => boolean
}

defineProps<Props>()
</script>

<template>
  <div v-if="reminders.length === 0" class="py-8">
    <EmptyState
      variant="notifications"
      title="暂无提醒事项"
      description="当前没有符合条件的提醒事项"
    />
  </div>

  <div v-else class="space-y-3">
    <div
      v-for="reminder in reminders"
      :key="reminder.reminderId"
      class="flex items-start gap-4 p-4 border rounded-lg hover:bg-muted/50 transition-colors"
    >
      <!-- Icon -->
      <div class="flex-shrink-0 mt-1">
        <div
          :class="[
            'flex h-10 w-10 items-center justify-center rounded-lg',
            isOverdue(reminder.targetDate) ? 'bg-red-100' : 'bg-blue-100'
          ]"
        >
          <SafeIcon
            :name="isOverdue(reminder.targetDate) ? 'AlertCircle' : 'Clock'"
            :size="20"
            :class="isOverdue(reminder.targetDate) ? 'text-red-600' : 'text-blue-600'"
          />
        </div>
      </div>

      <!-- Content -->
      <div class="flex-1 min-w-0">
        <div class="flex items-start justify-between gap-2">
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-1">
              <h3 class="font-medium text-sm">{{ reminder.title }}</h3>
              <Badge :class="getReminderTypeColor(reminder.type)" variant="secondary">
                {{ reminder.type }}
              </Badge>
              <Badge v-if="isOverdue(reminder.targetDate)" variant="destructive">
                已逾期
              </Badge>
            </div>
            <p class="text-sm text-muted-foreground">
              案件：{{ getCaseName(reminder.caseId) }}
            </p>
            <p class="text-xs text-muted-foreground mt-1">
              提醒时间：{{ formatDateTime(reminder.targetDate) }}
            </p>
            <div class="flex items-center gap-2 mt-2">
              <span class="text-xs text-muted-foreground">提醒方式：</span>
              <div class="flex gap-1">
                <Badge v-for="method in reminder.reminderMethod" :key="method" variant="outline" class="text-xs">
                  {{ method }}
                </Badge>
              </div>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex-shrink-0 flex gap-2">
            <Button
              as="a"
              :href="`./case-detail-view.html?id=${reminder.caseId}`"
              variant="ghost"
              size="sm"
            >
              <SafeIcon name="ChevronRight" :size="16" />
            </Button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
