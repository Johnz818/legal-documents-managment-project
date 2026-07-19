
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Button } from '@/components/ui/button'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import PageHeader from '@/components/common/PageHeader.vue'
import SafeIcon from '@/components/common/SafeIcon.vue'
import CaseDetailHeader from '@/components/case-detail-view/CaseDetailHeader.vue'
import CaseDetailBasicInfo from '@/components/case-detail-view/CaseDetailBasicInfo.vue'
import CaseDetailParties from '@/components/case-detail-view/CaseDetailParties.vue'
import CaseDetailDates from '@/components/case-detail-view/CaseDetailDates.vue'
import CaseDetailDocuments from '@/components/case-detail-view/CaseDetailDocuments.vue'
import CaseDetailReminders from '@/components/case-detail-view/CaseDetailReminders.vue'
import CaseDetailTeam from '@/components/case-detail-view/CaseDetailTeam.vue'
import { getCaseById } from '@/data/case'
import type { CaseModel } from '@/data/case'

interface Props {
  caseId: string
}

const props = defineProps<Props>()

const currentCaseId = ref(props.caseId)
const caseData = ref<CaseModel | null>(getCaseById(props.caseId) || null)
const isEditing = ref(false)
const activeTab = ref('basic')

// Handle query parameter override on client side
onMounted(() => {
  if (typeof window !== 'undefined') {
    const params = new URLSearchParams(window.location.search)
    const queryId = params.get('id')
    if (queryId) {
      currentCaseId.value = queryId
      const newCase = getCaseById(queryId)
      if (newCase) {
        caseData.value = newCase
      }
    }
  }
})

const breadcrumbs = computed(() => [
  { label: '案件管理', href: './case-list-view.html' },
  { label: caseData.value?.caseNumber || '案件详情' },
])

const handleEdit = () => {
  isEditing.value = true
}

const handleSave = () => {
  isEditing.value = false
  // In a real app, would save to backend
}

const handleCancel = () => {
  isEditing.value = false
}

const handleGenerateDocument = () => {
  // Navigate to document generation with case pre-selected
  if (typeof window !== 'undefined') {
    window.location.href = `./document-generate-selection.html?caseId=${currentCaseId.value}`
  }
}

const handleSetReminder = () => {
  // Navigate to reminder settings
  if (typeof window !== 'undefined') {
    window.location.href = `./case-detail-view-reminder-settings.html?caseId=${currentCaseId.value}`
  }
}

const handleViewAllReminders = () => {
  // Navigate to reminder dashboard
  if (typeof window !== 'undefined') {
    window.location.href = './reminder-dashboard.html'
  }
}
</script>

<template>
  <div class="flex flex-col h-full">
    <PageHeader
      title="案件详情"
      :breadcrumbs="breadcrumbs"
    >
      <template #actions>
        <Button
          v-if="!isEditing"
          variant="outline"
          size="sm"
          @click="handleEdit"
        >
          <SafeIcon name="Edit" :size="16" class="mr-2" />
          编辑
        </Button>
        <template v-else>
          <Button
            variant="default"
            size="sm"
            @click="handleSave"
          >
            <SafeIcon name="Check" :size="16" class="mr-2" />
            保存
          </Button>
          <Button
            variant="outline"
            size="sm"
            @click="handleCancel"
          >
            取消
          </Button>
        </template>
      </template>
    </PageHeader>

    <div class="flex-1 overflow-auto">
      <div class="container mx-auto px-4 py-6 space-y-6">
        <!-- Case Header with Status -->
        <CaseDetailHeader
          v-if="caseData"
          :case-data="caseData"
          :is-editing="isEditing"
        />

        <!-- Action Buttons -->
        <div class="flex flex-wrap gap-2">
          <Button
            variant="default"
            size="sm"
            @click="handleGenerateDocument"
          >
            <SafeIcon name="FileText" :size="16" class="mr-2" />
            生成文书
          </Button>
          <Button
            variant="outline"
            size="sm"
            @click="handleSetReminder"
          >
            <SafeIcon name="Bell" :size="16" class="mr-2" />
            设置提醒
          </Button>
          <Button
            variant="outline"
            size="sm"
            @click="handleViewAllReminders"
          >
            <SafeIcon name="Clock" :size="16" class="mr-2" />
            查看所有提醒
          </Button>
        </div>

        <!-- Tabs for Different Sections -->
        <Tabs v-model="activeTab" class="w-full">
          <TabsList class="grid w-full grid-cols-5">
            <TabsTrigger value="basic">基本信息</TabsTrigger>
            <TabsTrigger value="parties">当事人</TabsTrigger>
            <TabsTrigger value="dates">关键日期</TabsTrigger>
            <TabsTrigger value="documents">文书</TabsTrigger>
            <TabsTrigger value="reminders">提醒</TabsTrigger>
          </TabsList>

          <!-- Basic Info Tab -->
          <TabsContent value="basic" class="space-y-4">
            <CaseDetailBasicInfo
              v-if="caseData"
              :case-data="caseData"
              :is-editing="isEditing"
            />
          </TabsContent>

          <!-- Parties Tab -->
          <TabsContent value="parties" class="space-y-4">
            <CaseDetailParties
              v-if="caseData"
              :case-data="caseData"
              :is-editing="isEditing"
            />
          </TabsContent>

          <!-- Dates Tab -->
          <TabsContent value="dates" class="space-y-4">
            <CaseDetailDates
              v-if="caseData"
              :case-data="caseData"
              :is-editing="isEditing"
            />
          </TabsContent>

          <!-- Documents Tab -->
          <TabsContent value="documents" class="space-y-4">
            <CaseDetailDocuments
              v-if="caseData"
              :case-id="currentCaseId"
            />
          </TabsContent>

          <!-- Reminders Tab -->
          <TabsContent value="reminders" class="space-y-4">
            <CaseDetailReminders
              v-if="caseData"
              :case-id="currentCaseId"
            />
          </TabsContent>
        </Tabs>

        <!-- Team Section -->
        <CaseDetailTeam
          v-if="caseData"
          :case-data="caseData"
          :is-editing="isEditing"
        />
      </div>
    </div>
  </div>
</template>
