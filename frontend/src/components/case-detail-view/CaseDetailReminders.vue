
<script setup lang="ts">
import { computed } from 'vue'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Empty, EmptyHeader, EmptyTitle, EmptyDescription, EmptyContent } from '@/components/ui/empty'
import SafeIcon from '@/components/common/SafeIcon.vue'
import { MOCK_REMINDERS, ReminderType, ReminderMethod } from '@/data/reminder'

interface Props {
  caseId: string
}

const props = defineProps<Props>()

const caseReminders = computed(() => {
  return MOCK_REMINDERS.filter(reminder => reminder.caseId === props.caseId)
})

const getReminderTypeColor = (type: ReminderType) => {
  switch (type) {
    case ReminderType.KeyDate:
      return 'bg-red-100 text-red-800'
    case ReminderType.Custom:
      return 'bg-blue-100 text-blue-800'
    case ReminderType.SystemAlert:
      return 'bg-yellow-100 text-yellow-800'
    default:
      return 'bg-gray-100 text-gray-800'
  }
}

const getReminderMethodLabel = (methods: ReminderMethod[]) => {
  return methods.map(m => {
    switch (m) {
      case ReminderMethod.System:
        return '系统'
      case ReminderMethod.Email:
        return '邮件'
      case ReminderMethod.Both:
        return '系统+邮件'
      default:
        return m
    }
  }).join(', ')
}

const handleEditReminder = () => {
  if (typeof window !== 'undefined') {
    window.location.href = `./case-detail-view-reminder-settings.html?caseId=${props.caseId}`
  }
}
</script>

<template>
  <Card>
    <CardHeader class="flex flex-row items-center justify-between">
      <CardTitle>提醒事项</CardTitle>
      <Button
        variant="outline"
        size="sm"
        @click="handleEditReminder"
      >
        <SafeIcon name="Edit" :size="16" class="mr-2" />
        编辑提醒
      </Button>
    </CardHeader>
    <CardContent>
      <div v-if="caseReminders.length > 0" class="space-y-4">
        <div
          v-for="reminder in caseReminders"
          :key="reminder.reminderId"
          class="flex items-start justify-between p-4 border rounded-lg hover:bg-muted/50 transition-colors"
        >
          <div class="flex items-start gap-3 flex-1">
            <SafeIcon
              :name="reminder.isCompleted ? 'CheckCircle' : 'Clock'"
              :size="20"
              :class="reminder.isCompleted ? 'text-green-600' : 'text-orange-600'"
            />
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <p class="font-medium">{{ reminder.title }}</p>
                <Badge :class="getReminderTypeColor(reminder.type)" class="text-xs">
                  {{ reminder.type }}
                </Badge>
                <Badge v-if="reminder.isCompleted" variant="secondary" class="text-xs">
                  已完成
                </Badge>
              </div>
              <p class="text-xs text-muted-foreground mb-2">
                提醒时间: {{ reminder.targetDate }}
              </p>
              <p class="text-xs text-muted-foreground">
                通知方式: {{ getReminderMethodLabel(reminder.reminderMethod) }}
              </p>
            </div>
          </div>
        </div>
      </div>
      <div v-else>
        <Empty class="py-8">
          <EmptyHeader>
            <div class="flex h-16 w-16 items-center justify-center rounded-full bg-muted mx-auto mb-4">
              <SafeIcon name="Bell" :size="32" class="text-muted-foreground" />
            </div>
            <EmptyTitle>暂无提醒</EmptyTitle>
            <EmptyDescription>该案件还没有设置任何提醒事项</EmptyDescription>
          </EmptyHeader>
          <EmptyContent>
            <Button
              size="sm"
              @click="handleEditReminder"
            >
              设置提醒
            </Button>
          </EmptyContent>
        </Empty>
      </div>
    </CardContent>
  </Card>
</template>
