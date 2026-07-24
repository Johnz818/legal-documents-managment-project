
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
import { getCaseById } from '@/services/caseService'
import type { CaseDetailResponse } from '@/types/case'

interface Props {
  caseId: string
}

const props = defineProps<Props>()

const currentCaseId = ref<number | null>(null)
const caseData = ref<CaseDetailResponse | null>(null)
const isLoading = ref(true)
const isNotFound = ref(false)
const errorMessage = ref('')
const isEditing = ref(false)
const activeTab = ref('basic')

const relatedCaseId = computed(() => currentCaseId.value?.toString() ?? '')

const resolveCaseId = () => {
  const queryId = typeof window === 'undefined'
    ? null
    : new URLSearchParams(window.location.search).get('id')
  const rawCaseId = queryId || props.caseId

  if (!/^[1-9]\d*$/.test(rawCaseId)) {
    return null
  }

  const parsedCaseId = Number(rawCaseId)
  return Number.isSafeInteger(parsedCaseId) ? parsedCaseId : null
}

const loadCase = async () => {
  isLoading.value = true
  isNotFound.value = false
  errorMessage.value = ''
  caseData.value = null

  const caseId = resolveCaseId()
  currentCaseId.value = caseId

  if (caseId === null) {
    isNotFound.value = true
    isLoading.value = false
    return
  }

  try {
    const result = await getCaseById(caseId)

    if (result === null) {
      isNotFound.value = true
    } else {
      caseData.value = result
    }
  } catch {
    errorMessage.value = '案件详情加载失败，请确认后端服务可用后重试。'
  } finally {
    isLoading.value = false
  }
}

onMounted(loadCase)

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
  if (typeof window !== 'undefined' && currentCaseId.value !== null) {
    window.location.href = `./document-generate-selection.html?caseId=${currentCaseId.value}`
  }
}

const handleSetReminder = () => {
  // Navigate to reminder settings
  if (typeof window !== 'undefined' && currentCaseId.value !== null) {
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
          v-if="caseData && !isEditing"
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
        <div v-if="isLoading" class="flex h-96 items-center justify-center">
          <div class="flex items-center gap-2 text-sm text-muted-foreground">
            <SafeIcon name="LoaderCircle" :size="18" class="animate-spin" />
            正在加载案件详情...
          </div>
        </div>

        <div v-else-if="isNotFound" class="flex h-96 items-center justify-center">
          <Card class="w-full max-w-md text-center">
            <CardHeader>
              <CardTitle>未找到案件</CardTitle>
              <CardDescription>案件不存在，或链接中的案件编号无效。</CardDescription>
            </CardHeader>
            <CardContent>
              <Button as="a" href="./case-list-view.html" variant="outline">
                返回案件列表
              </Button>
            </CardContent>
          </Card>
        </div>

        <div v-else-if="errorMessage" class="flex h-96 items-center justify-center">
          <div class="flex max-w-md flex-col items-center gap-3 text-center">
            <SafeIcon name="CircleAlert" :size="28" class="text-destructive" />
            <p class="text-sm text-muted-foreground">{{ errorMessage }}</p>
            <Button variant="outline" size="sm" @click="loadCase">
              重新加载
            </Button>
          </div>
        </div>

        <template v-else-if="caseData">
        <!-- Case Header with Status -->
        <CaseDetailHeader
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
              :case-data="caseData"
              :is-editing="isEditing"
            />
          </TabsContent>

          <!-- Parties Tab -->
          <TabsContent value="parties" class="space-y-4">
            <CaseDetailParties
              :case-data="caseData"
              :is-editing="isEditing"
            />
          </TabsContent>

          <!-- Dates Tab -->
          <TabsContent value="dates" class="space-y-4">
            <CaseDetailDates
              :case-data="caseData"
              :is-editing="isEditing"
            />
          </TabsContent>

          <!-- Documents Tab -->
          <TabsContent value="documents" class="space-y-4">
            <CaseDetailDocuments
              :case-id="relatedCaseId"
            />
          </TabsContent>

          <!-- Reminders Tab -->
          <TabsContent value="reminders" class="space-y-4">
            <CaseDetailReminders
              :case-id="relatedCaseId"
            />
          </TabsContent>
        </Tabs>

        <!-- Team Section -->
        <CaseDetailTeam
          :case-data="caseData"
          :is-editing="isEditing"
        />
        </template>
      </div>
    </div>
  </div>
</template>
