
<script setup lang="ts">
import { computed } from 'vue'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'
import { ReminderType, ReminderMethod, type ReminderModel } from '@/data/reminder'

interface Props {
  reminders: ReminderModel[]
  readonly?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  readonly: false,
})

const emit = defineEmits<{
  edit: [reminder: ReminderModel]
  delete: [reminderId: string]
}>()

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

const getReminderMethodIcon = (method: ReminderMethod) => {
  switch (method) {
    case ReminderMethod.System:
      return 'Bell'
    case ReminderMethod.Email:
      return 'Mail'
    case ReminderMethod.Both:
      return 'Send'
    default:
      return 'AlertCircle'
  }
}

const formatDate = (dateStr: string) => {
  try {
    const date = new Date(dateStr)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return dateStr
  }
}

const isOverdue = (dateStr: string) => {
  try {
    return new Date(dateStr) < new Date()
  } catch {
    return false
  }
}
</script>

<template>
  <div class="space-y-3">
    <div v-if="reminders.length === 0" class="text-center py-8">
      <SafeIcon name="Inbox" :size="40" class="mx-auto text-muted-foreground mb-2" />
      <p class="text-muted-foreground">暂无提醒事项</p>
    </div>

    <Card 
      v-for="reminder in reminders" 
      :key="reminder.reminderId"
      :class="[
        'transition-all',
        isOverdue(reminder.targetDate) && !reminder.isCompleted ? 'border-destructive/50 bg-destructive/5' : ''
      ]"
    >
      <CardContent class="pt-6">
        <div class="flex items-start justify-between gap-4">
          <!-- Left Content -->
          <div class="flex-1 space-y-2">
            <!-- Title and Type -->
            <div class="flex items-center gap-2">
              <h3 class="font-semibold">{{ reminder.title }}</h3>
              <Badge :class="getReminderTypeColor(reminder.type)" variant="secondary">
                {{ reminder.type }}
              </Badge>
              <Badge v-if="reminder.isCompleted" variant="outline">已完成</Badge>
              <Badge v-if="isOverdue(reminder.targetDate) && !reminder.isCompleted" variant="destructive">
                已逾期
              </Badge>
            </div>

            <!-- Details -->
            <div class="grid grid-cols-2 gap-4 text-sm">
              <div>
                <p class="text-muted-foreground">提醒时间</p>
                <p class="font-medium">{{ formatDate(reminder.targetDate) }}</p>
              </div>
              <div>
                <p class="text-muted-foreground">关联阶段</p>
                <p class="font-medium">{{ reminder.linkedStage }}</p>
              </div>
            </div>

            <!-- Reminder Methods -->
            <div class="flex items-center gap-2 pt-2">
              <span class="text-sm text-muted-foreground">提醒方式：</span>
              <div class="flex gap-1">
                <Badge 
                  v-for="method in reminder.reminderMethod" 
                  :key="method"
                  variant="outline"
                  class="flex items-center gap-1"
                >
                  <SafeIcon :name="getReminderMethodIcon(method)" :size="14" />
                  {{ method }}
                </Badge>
              </div>
            </div>
          </div>

          <!-- Actions -->
          <div v-if="!readonly" class="flex gap-2">
            <Button 
              variant="ghost" 
              size="sm"
              @click="emit('edit', reminder)"
            >
              <SafeIcon name="Edit2" :size="16" />
            </Button>
            <Button 
              variant="ghost" 
              size="sm"
              class="text-destructive hover:text-destructive"
              @click="emit('delete', reminder.reminderId)"
            >
              <SafeIcon name="Trash2" :size="16" />
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  </div>
</template>
