
<script setup lang="ts">
import type { ReminderSummaryModel } from '@/data/reminder'
import { Card, CardContent } from '@/components/ui/card'
import SafeIcon from '@/components/common/SafeIcon.vue'

interface Props {
  summary: ReminderSummaryModel
}

defineProps<Props>()
</script>

<template>
  <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
    <!-- Today Reminders Card -->
    <Card class="border-l-4 border-l-blue-500">
      <CardContent class="pt-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-muted-foreground">今日提醒</p>
            <p class="text-3xl font-bold mt-2">{{ summary.todayReminders }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-lg bg-blue-100">
            <SafeIcon name="Calendar" :size="24" class="text-blue-600" />
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- Upcoming Reminders Card -->
    <Card class="border-l-4 border-l-green-500">
      <CardContent class="pt-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-muted-foreground">即将到来</p>
            <p class="text-3xl font-bold mt-2">{{ summary.upcomingReminders }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-lg bg-green-100">
            <SafeIcon name="Clock" :size="24" class="text-green-600" />
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- Overdue Reminders Card -->
    <Card class="border-l-4 border-l-red-500">
      <CardContent class="pt-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-muted-foreground">已逾期</p>
            <p class="text-3xl font-bold mt-2">{{ summary.overdueReminders }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-lg bg-red-100">
            <SafeIcon name="AlertCircle" :size="24" class="text-red-600" />
          </div>
        </div>
      </CardContent>
    </Card>
  </div>
</template>
