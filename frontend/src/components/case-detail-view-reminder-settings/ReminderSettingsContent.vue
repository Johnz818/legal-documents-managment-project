
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Alert, AlertDescription } from '@/components/ui/alert'
import SafeIcon from '@/components/common/SafeIcon.vue'
import { MOCK_REMINDERS, ReminderType } from '@/data/reminder'
import { getCaseById } from '@/data/case'
import ReminderList from './ReminderList.vue'
import ReminderForm from './ReminderForm.vue'

// Get case ID from URL query parameter (with fallback to first case)
const caseId = ref('C2024001')
const currentCase = computed(() => getCaseById(caseId.value))
const caseReminders = computed(() => 
  MOCK_REMINDERS.filter(r => r.caseId === caseId.value)
)

const showAddForm = ref(false)
const editingReminder = ref<typeof MOCK_REMINDERS[0] | null>(null)

const handleAddReminder = () => {
  editingReminder.value = null
  showAddForm.value = true
}

const handleEditReminder = (reminder: typeof MOCK_REMINDERS[0]) => {
  editingReminder.value = reminder
  showAddForm.value = true
}

const handleCloseForm = () => {
  showAddForm.value = false
  editingReminder.value = null
}

const handleSaveReminder = (data: any) => {
  // In a real app, this would save to backend
  console.log('Saving reminder:', data)
  handleCloseForm()
}

const handleDeleteReminder = (reminderId: string) => {
  // In a real app, this would delete from backend
  console.log('Deleting reminder:', reminderId)
}

onMounted(() => {
  if (typeof window !== 'undefined') {
    const params = new URLSearchParams(window.location.search)
    const queryId = params.get('id')
    if (queryId) {
      caseId.value = queryId
    }
  }
})

const autoReminderCount = computed(() => 
  caseReminders.value.filter(r => r.type === ReminderType.KeyDate).length
)

const customReminderCount = computed(() => 
  caseReminders.value.filter(r => r.type === ReminderType.Custom).length
)
</script>

<template>
  <div class="container mx-auto px-4 py-6 max-w-4xl">
    <!-- Case Info Card -->
    <Card class="mb-6">
      <CardHeader>
        <CardTitle class="flex items-center gap-2">
          <SafeIcon name="AlertCircle" :size="20" class="text-primary" />
          案件信息
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div v-if="currentCase" class="grid grid-cols-2 gap-4">
          <div>
            <p class="text-sm text-muted-foreground">案号</p>
            <p class="font-semibold">{{ currentCase.caseNumber }}</p>
          </div>
          <div>
            <p class="text-sm text-muted-foreground">法院</p>
            <p class="font-semibold">{{ currentCase.courtName }}</p>
          </div>
          <div>
            <p class="text-sm text-muted-foreground">当事人</p>
            <p class="font-semibold">{{ currentCase.plaintiff }} vs {{ currentCase.defendant }}</p>
          </div>
          <div>
            <p class="text-sm text-muted-foreground">案件阶段</p>
            <p class="font-semibold">{{ currentCase.caseStage }}</p>
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- Reminder Statistics -->
    <div class="grid grid-cols-2 gap-4 mb-6">
      <Card>
        <CardHeader class="pb-3">
          <CardTitle class="text-sm font-medium">自动生成提醒</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-bold">{{ autoReminderCount }}</div>
          <p class="text-xs text-muted-foreground mt-1">基于案件阶段自动生成</p>
        </CardContent>
      </Card>
      <Card>
        <CardHeader class="pb-3">
          <CardTitle class="text-sm font-medium">自定义提醒</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-bold">{{ customReminderCount }}</div>
          <p class="text-xs text-muted-foreground mt-1">用户手动添加</p>
        </CardContent>
      </Card>
    </div>

    <!-- Tabs for Auto and Custom Reminders -->
    <Tabs defaultValue="all" class="mb-6">
      <TabsList class="grid w-full grid-cols-3">
        <TabsTrigger value="all">全部提醒 ({{ caseReminders.length }})</TabsTrigger>
        <TabsTrigger value="auto">自动提醒 ({{ autoReminderCount }})</TabsTrigger>
        <TabsTrigger value="custom">自定义 ({{ customReminderCount }})</TabsTrigger>
      </TabsList>

      <!-- All Reminders Tab -->
      <TabsContent value="all" class="space-y-4">
        <ReminderList 
          :reminders="caseReminders"
          @edit="handleEditReminder"
          @delete="handleDeleteReminder"
        />
      </TabsContent>

      <!-- Auto Reminders Tab -->
      <TabsContent value="auto" class="space-y-4">
        <ReminderList 
          :reminders="caseReminders.filter(r => r.type === ReminderType.KeyDate)"
          :readonly="true"
          @edit="handleEditReminder"
          @delete="handleDeleteReminder"
        />
        <Alert v-if="autoReminderCount === 0">
          <SafeIcon name="Info" :size="16" />
          <AlertDescription>
            该案件暂无自动生成的提醒。系统会根据案件阶段变化自动生成相应提醒。
          </AlertDescription>
        </Alert>
      </TabsContent>

      <!-- Custom Reminders Tab -->
      <TabsContent value="custom" class="space-y-4">
        <ReminderList 
          :reminders="caseReminders.filter(r => r.type === ReminderType.Custom)"
          @edit="handleEditReminder"
          @delete="handleDeleteReminder"
        />
        <Alert v-if="customReminderCount === 0">
          <SafeIcon name="Info" :size="16" />
          <AlertDescription>
            您还没有添加自定义提醒。点击下方按钮添加新的提醒事项。
          </AlertDescription>
        </Alert>
      </TabsContent>
    </Tabs>

    <!-- Add/Edit Form -->
    <Card v-if="showAddForm" class="mb-6 border-primary/50 bg-primary/5">
      <CardHeader>
        <CardTitle>{{ editingReminder ? '编辑提醒' : '添加新提醒' }}</CardTitle>
        <CardDescription>
          {{ editingReminder ? '修改提醒的内容和方式' : '为案件添加自定义提醒事项' }}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <ReminderForm 
          :reminder="editingReminder"
          :case-id="caseId"
          @save="handleSaveReminder"
          @cancel="handleCloseForm"
        />
      </CardContent>
    </Card>

    <!-- Add Button (when form is hidden) -->
    <div v-if="!showAddForm" class="flex gap-2 justify-end">
      <Button variant="outline" as="a" href="./case-detail-view.html">
        <SafeIcon name="ArrowLeft" :size="16" class="mr-2" />
        返回案件详情
      </Button>
      <Button @click="handleAddReminder">
        <SafeIcon name="Plus" :size="16" class="mr-2" />
        添加提醒事项
      </Button>
    </div>

    <!-- Form Buttons (when form is shown) -->
    <div v-else class="flex gap-2 justify-end">
      <Button variant="outline" @click="handleCloseForm">
        取消
      </Button>
      <Button @click="() => handleSaveReminder({})">
        保存提醒
      </Button>
    </div>
  </div>
</template>
