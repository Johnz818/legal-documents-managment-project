
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'
import CalendarHeader from '@/components/case-calendar-view/CalendarHeader.vue'
import CalendarEventItem from '@/components/case-calendar-view/CalendarEventItem.vue'
import { MOCK_CALENDAR_EVENTS } from '@/data/reminder'
import { MOCK_CASES } from '@/data/case'
import type { CalendarEventModel } from '@/data/reminder'

const currentDate = ref(new Date(2024, 11, 14)) // December 14, 2024
const selectedDate = ref<Date | null>(null)
const calendarEvents = ref<CalendarEventModel[]>(MOCK_CALENDAR_EVENTS)

// Get days in month
const daysInMonth = computed(() => {
  return new Date(currentDate.value.getFullYear(), currentDate.value.getMonth() + 1, 0).getDate()
})

// Get first day of month (0 = Sunday, 1 = Monday, etc.)
const firstDayOfMonth = computed(() => {
  return new Date(currentDate.value.getFullYear(), currentDate.value.getMonth(), 1).getDay()
})

// Get all calendar days (including previous/next month padding)
const calendarDays = computed(() => {
  const days: (number | null)[] = []
  
  // Add padding for previous month
  for (let i = 0; i < firstDayOfMonth.value; i++) {
    days.push(null)
  }
  
  // Add days of current month
  for (let i = 1; i <= daysInMonth.value; i++) {
    days.push(i)
  }
  
  // Add padding for next month
  const remainingDays = 42 - days.length // 6 rows × 7 days
  for (let i = 1; i <= remainingDays; i++) {
    days.push(null)
  }
  
  return days
})

// Get events for a specific date
const getEventsForDate = (day: number | null) => {
  if (!day) return []
  
  const dateStr = `${currentDate.value.getFullYear()}-${String(currentDate.value.getMonth() + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  return calendarEvents.value.filter(event => event.start.startsWith(dateStr))
}

// Get selected date events
const selectedDateEvents = computed(() => {
  if (!selectedDate.value) return []
  
  const day = selectedDate.value.getDate()
  return getEventsForDate(day)
})

// Get case name by ID
const getCaseName = (caseId: string) => {
  return MOCK_CASES.find(c => c.id === caseId)?.caseNumber || caseId
}

// Navigation handlers
const previousMonth = () => {
  currentDate.value = new Date(currentDate.value.getFullYear(), currentDate.value.getMonth() - 1)
}

const nextMonth = () => {
  currentDate.value = new Date(currentDate.value.getFullYear(), currentDate.value.getMonth() + 1)
}

const selectDate = (day: number | null) => {
  if (!day) return
  selectedDate.value = new Date(currentDate.value.getFullYear(), currentDate.value.getMonth(), day)
}

const isToday = (day: number | null) => {
  if (!day) return false
  const today = new Date()
  return (
    day === today.getDate() &&
    currentDate.value.getMonth() === today.getMonth() &&
    currentDate.value.getFullYear() === today.getFullYear()
  )
}

const isSelected = (day: number | null) => {
  if (!day || !selectedDate.value) return false
  return (
    day === selectedDate.value.getDate() &&
    currentDate.value.getMonth() === selectedDate.value.getMonth() &&
    currentDate.value.getFullYear() === selectedDate.value.getFullYear()
  )
}

const monthYear = computed(() => {
  const months = ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月']
  return `${currentDate.value.getFullYear()}年 ${months[currentDate.value.getMonth()]}`
})

const weekDays = ['日', '一', '二', '三', '四', '五', '六']
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Calendar -->
      <Card class="lg:col-span-2">
        <CardHeader class="pb-3">
          <div class="flex items-center justify-between">
            <CardTitle>{{ monthYear }}</CardTitle>
            <div class="flex gap-2">
              <Button variant="outline" size="icon" @click="previousMonth">
                <SafeIcon name="ChevronLeft" :size="20" />
              </Button>
              <Button variant="outline" size="icon" @click="nextMonth">
                <SafeIcon name="ChevronRight" :size="20" />
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <!-- Week days header -->
          <div class="grid grid-cols-7 gap-1 mb-2">
            <div
              v-for="day in weekDays"
              :key="day"
              class="text-center text-sm font-semibold text-muted-foreground py-2"
            >
              {{ day }}
            </div>
          </div>

          <!-- Calendar grid -->
          <div class="grid grid-cols-7 gap-1">
            <button
              v-for="(day, index) in calendarDays"
              :key="index"
              @click="selectDate(day)"
              :class="[
                'relative aspect-square p-2 rounded-lg border text-sm transition-colors',
                day === null ? 'bg-muted/30 cursor-default' : 'hover:bg-accent cursor-pointer',
                isToday(day) ? 'border-primary bg-primary/10' : 'border-border',
                isSelected(day) ? 'bg-primary text-primary-foreground border-primary' : '',
              ]"
            >
              <div v-if="day" class="text-right font-medium">{{ day }}</div>
              
              <!-- Event indicators -->
              <div v-if="day" class="absolute bottom-1 left-1 right-1 flex gap-0.5 flex-wrap justify-center">
                <div
                  v-for="(event, idx) in getEventsForDate(day).slice(0, 2)"
                  :key="idx"
                  :style="{ backgroundColor: event.color }"
                  class="h-1.5 w-1.5 rounded-full"
                />
                <div
                  v-if="getEventsForDate(day).length > 2"
                  class="h-1.5 w-1.5 rounded-full bg-muted-foreground"
                />
              </div>
            </button>
          </div>

          <!-- Legend -->
          <div class="mt-4 pt-4 border-t space-y-2">
            <div class="text-xs font-semibold text-muted-foreground">图例</div>
            <div class="flex flex-wrap gap-4 text-xs">
              <div class="flex items-center gap-2">
                <div class="h-2 w-2 rounded-full bg-red-500" />
                <span>关键日期提醒</span>
              </div>
              <div class="flex items-center gap-2">
                <div class="h-2 w-2 rounded-full bg-blue-500" />
                <span>自定义事项</span>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- Events sidebar -->
      <Card class="h-fit">
        <CardHeader>
          <CardTitle class="text-base">
            {{ selectedDate ? `${selectedDate.getMonth() + 1}月${selectedDate.getDate()}日` : '选择日期' }}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div v-if="selectedDateEvents.length === 0" class="text-center py-8">
            <SafeIcon name="Calendar" :size="32" class="mx-auto text-muted-foreground mb-2" />
            <p class="text-sm text-muted-foreground">该日期暂无事项</p>
          </div>

          <div v-else class="space-y-3">
            <CalendarEventItem
              v-for="event in selectedDateEvents"
              :key="event.id"
              :event="event"
              :case-name="getCaseName(event.caseId)"
            />
          </div>

          <!-- Return button -->
          <Button
            variant="outline"
            class="w-full mt-4"
            as="a"
            href="./reminder-dashboard.html"
          >
            <SafeIcon name="ArrowLeft" :size="16" class="mr-2" />
            返回提醒仪表盘
          </Button>
        </CardContent>
      </Card>
    </div>
  </div>
</template>

<style scoped>
/* Calendar grid responsive */
@media (max-width: 1024px) {
  :deep(.grid) {
    gap: 0.5rem;
  }
}
</style>
