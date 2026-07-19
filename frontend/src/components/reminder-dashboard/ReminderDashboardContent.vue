
<script setup lang="ts">
import { ref, computed } from 'vue'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import SafeIcon from '@/components/common/SafeIcon.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import ReminderSummaryCards from '@/components/reminder-dashboard/ReminderSummaryCards.vue'
import ReminderQuickActions from '@/components/reminder-dashboard/ReminderQuickActions.vue'
import ReminderListSection from '@/components/reminder-dashboard/ReminderListSection.vue'
import { MOCK_REMINDERS, MOCK_REMINDER_SUMMARY, ReminderType } from '@/data/reminder'
import { MOCK_CASES } from '@/data/case'

const selectedTab = ref('all')

// 获取案件名称
function getCaseName(caseId: string): string {
  const caseItem = MOCK_CASES.find(c => c.id === caseId)
  return caseItem ? caseItem.caseNumber : '未知案件'
}

// 按类型筛选提醒
const filteredReminders = computed(() => {
  if (selectedTab.value === 'all') {
    return MOCK_REMINDERS
  }
  return MOCK_REMINDERS.filter(r => r.type === selectedTab.value)
})

// 按日期排序（最近的在前）
const sortedReminders = computed(() => {
  return [...filteredReminders.value].sort((a, b) => {
    return new Date(b.targetDate).getTime() - new Date(a.targetDate).getTime()
  })
})

// 获取提醒类型标签颜色
function getReminderTypeColor(type: ReminderType): string {
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

// 格式化日期时间
function formatDateTime(dateStr: string): string {
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// 判断是否逾期
function isOverdue(targetDate: string): boolean {
  return new Date(targetDate) < new Date()
}
</script>

<template>
  <div class="flex flex-col h-full">
    <!-- Page Header -->
    <PageHeader
      title="提醒事项仪表盘"
      description="查看所有案件的关键时间节点和自定义提醒事项"
      :breadcrumbs="[
        { label: '首页', href: './case-list-view.html' },
        { label: '提醒事项仪表盘' }
      ]"
    >
      <template #actions>
        <Button as="a" href="./case-list-view.html" variant="outline" size="sm">
          <SafeIcon name="ArrowLeft" :size="16" class="mr-2" />
          返回案件列表
        </Button>
      </template>
    </PageHeader>

    <!-- Main Content -->
    <div class="flex-1 overflow-auto">
      <div class="container mx-auto px-4 py-6 space-y-6">
        <!-- Summary Cards -->
        <ReminderSummaryCards :summary="MOCK_REMINDER_SUMMARY" />

        <!-- Quick Actions -->
        <ReminderQuickActions />

        <!-- Reminders List Section -->
        <Card>
          <CardHeader>
            <CardTitle>提醒事项列表</CardTitle>
            <CardDescription>
              共 {{ MOCK_REMINDERS.length }} 个提醒事项
            </CardDescription>
          </CardHeader>
          <CardContent>
            <!-- Tabs for filtering -->
            <Tabs v-model="selectedTab" class="w-full">
              <TabsList class="grid w-full grid-cols-4">
                <TabsTrigger value="all">全部</TabsTrigger>
                <TabsTrigger value="关键日期提醒">关键日期</TabsTrigger>
                <TabsTrigger value="自定义事项">自定义</TabsTrigger>
                <TabsTrigger value="系统预警提醒">系统预警</TabsTrigger>
              </TabsList>

              <!-- All Reminders Tab -->
              <TabsContent value="all" class="mt-4">
                <ReminderListSection
                  :reminders="sortedReminders"
                  :get-case-name="getCaseName"
                  :get-reminder-type-color="getReminderTypeColor"
                  :format-date-time="formatDateTime"
                  :is-overdue="isOverdue"
                />
              </TabsContent>

              <!-- Key Date Reminders Tab -->
              <TabsContent value="关键日期提醒" class="mt-4">
                <ReminderListSection
                  :reminders="sortedReminders"
                  :get-case-name="getCaseName"
                  :get-reminder-type-color="getReminderTypeColor"
                  :format-date-time="formatDateTime"
                  :is-overdue="isOverdue"
                />
              </TabsContent>

              <!-- Custom Reminders Tab -->
              <TabsContent value="自定义事项" class="mt-4">
                <ReminderListSection
                  :reminders="sortedReminders"
                  :get-case-name="getCaseName"
                  :get-reminder-type-color="getReminderTypeColor"
                  :format-date-time="formatDateTime"
                  :is-overdue="isOverdue"
                />
              </TabsContent>

              <!-- System Alert Tab -->
              <TabsContent value="系统预警提醒" class="mt-4">
                <ReminderListSection
                  :reminders="sortedReminders"
                  :get-case-name="getCaseName"
                  :get-reminder-type-color="getReminderTypeColor"
                  :format-date-time="formatDateTime"
                  :is-overdue="isOverdue"
                />
              </TabsContent>
            </Tabs>
          </CardContent>
        </Card>
      </div>
    </div>
  </div>
</template>
