
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import PageHeader from '@/components/common/PageHeader.vue'
import SafeIcon from '@/components/common/SafeIcon.vue'
import CaseDetailHeader from '@/components/case-detail-view/CaseDetailHeader.vue'
import CaseDetailBasicInfo from '@/components/case-detail-view/CaseDetailBasicInfo.vue'
import CaseDetailParties from '@/components/case-detail-view/CaseDetailParties.vue'
import CaseDetailDates from '@/components/case-detail-view/CaseDetailDates.vue'
import CaseDetailDocuments from '@/components/case-detail-view/CaseDetailDocuments.vue'
import CaseDetailReminders from '@/components/case-detail-view/CaseDetailReminders.vue'
import CaseDetailTeam from '@/components/case-detail-view/CaseDetailTeam.vue'
import {
  CaseUpdateNotFoundError,
  changeCaseArchiveState,
  DuplicateCaseNumberError,
  getCaseById,
  StaleCaseVersionError,
  updateCase,
} from '@/services/caseService'
import { CASE_STATUS_VALUES, getCaseStatusCode } from '@/constants/caseStatus'
import type { CaseDetailResponse, CaseUpdateRequest } from '@/types/case'

interface Props {
  caseId: string
}

const props = defineProps<Props>()

const currentCaseId = ref<number | null>(null)
const caseData = ref<CaseDetailResponse | null>(null)
const isLoading = ref(true)
const isNotFound = ref(false)
const errorMessage = ref('')
const updateErrorMessage = ref('')
const isEditing = ref(false)
const archiveDialogOpen = ref(false)
const isChangingArchiveState = ref(false)
const activeTab = ref('basic')

const validationSchema = z.object({
  caseNumber: z.string().trim().min(1, '案号不能为空').max(100, '案号不能超过100个字符'),
  caseName: z.string().trim().min(1, '案件名称不能为空').max(255, '案件名称不能超过255个字符'),
  courtName: z.string().trim().max(255, '法院名称不能超过255个字符').optional(),
  caseCause: z.string().trim().max(255, '案由不能超过255个字符').optional(),
  status: z.enum(CASE_STATUS_VALUES, {
    required_error: '案件阶段不能为空',
    invalid_type_error: '请选择支持的案件阶段',
  }),
  plaintiff: z.string().trim().min(1, '原告/申请人不能为空').max(255, '原告/申请人不能超过255个字符'),
  defendant: z.string().trim().min(1, '被告/被申请人不能为空').max(255, '被告/被申请人不能超过255个字符'),
  leadLawyerName: z.string().trim().min(1, '主办律师不能为空').max(255, '主办律师不能超过255个字符'),
  filingDate: z.string().optional(),
  hearingDate: z.string().optional(),
  judgmentDate: z.string().optional(),
  description: z.string().optional(),
})

const {
  handleSubmit,
  isSubmitting,
  resetForm,
} = useForm({
  validationSchema: toTypedSchema(validationSchema),
  initialValues: {
    caseNumber: '',
    caseName: '',
    courtName: '',
    caseCause: '',
    status: undefined,
    plaintiff: '',
    defendant: '',
    leadLawyerName: '',
    filingDate: '',
    hearingDate: '',
    judgmentDate: '',
    description: '',
  },
})

const relatedCaseId = computed(() => currentCaseId.value?.toString() ?? '')

const resetCaseForm = (detail: CaseDetailResponse) => {
  resetForm({
    values: {
      caseNumber: detail.caseNumber,
      caseName: detail.caseName,
      courtName: detail.courtName ?? '',
      caseCause: detail.caseCause ?? '',
      status: getCaseStatusCode(detail.status),
      plaintiff: detail.plaintiff,
      defendant: detail.defendant,
      leadLawyerName: detail.leadLawyerName,
      filingDate: detail.filingDate ?? '',
      hearingDate: detail.hearingDate ?? '',
      judgmentDate: detail.judgmentDate ?? '',
      description: detail.description ?? '',
    },
  })
}

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
  updateErrorMessage.value = ''
  isEditing.value = false
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
      resetCaseForm(result)
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
  if (caseData.value) {
    resetCaseForm(caseData.value)
  }
  updateErrorMessage.value = ''
  isEditing.value = true
}

const handleSave = handleSubmit(async (values) => {
  if (currentCaseId.value === null || caseData.value === null) {
    return
  }

  updateErrorMessage.value = ''
  const request: CaseUpdateRequest = {
    caseNumber: values.caseNumber,
    caseName: values.caseName,
    status: values.status,
    courtName: values.courtName || null,
    caseCause: values.caseCause || null,
    plaintiff: values.plaintiff,
    defendant: values.defendant,
    leadLawyerName: values.leadLawyerName,
    filingDate: values.filingDate || null,
    hearingDate: values.hearingDate || null,
    judgmentDate: values.judgmentDate || null,
    description: values.description?.trim() || null,
    version: caseData.value.version,
  }

  try {
    const updatedCase = await updateCase(currentCaseId.value, request)
    caseData.value = updatedCase
    resetCaseForm(updatedCase)
    isEditing.value = false
  } catch (error) {
    if (error instanceof DuplicateCaseNumberError) {
      updateErrorMessage.value = '该案号已属于其他案件，请检查后重新输入。'
    } else if (error instanceof StaleCaseVersionError) {
      updateErrorMessage.value = '案件已被其他用户修改，请重新加载最新内容后再编辑。'
    } else if (error instanceof CaseUpdateNotFoundError) {
      updateErrorMessage.value = '案件已不存在，无法保存本次修改。'
    } else {
      updateErrorMessage.value = '案件保存失败，请确认后端服务可用后重试。'
    }
  }
}, ({ errors }) => {
  const fieldNames = Object.keys(errors)
  if (fieldNames.some(name => ['plaintiff', 'defendant'].includes(name))) {
    activeTab.value = 'parties'
  } else if (fieldNames.some(name => ['filingDate', 'hearingDate', 'judgmentDate'].includes(name))) {
    activeTab.value = 'dates'
  } else {
    activeTab.value = 'basic'
  }
})

const handleCancel = () => {
  if (caseData.value) {
    resetCaseForm(caseData.value)
  }
  updateErrorMessage.value = ''
  isEditing.value = false
}

const handleArchiveStateChange = async () => {
  if (currentCaseId.value === null || caseData.value === null) {
    return
  }

  isChangingArchiveState.value = true
  updateErrorMessage.value = ''
  try {
    const updatedCase = await changeCaseArchiveState(
      currentCaseId.value,
      caseData.value.version,
      !caseData.value.archived,
    )
    caseData.value = updatedCase
    resetCaseForm(updatedCase)
    archiveDialogOpen.value = false
  } catch (error) {
    archiveDialogOpen.value = false
    if (error instanceof StaleCaseVersionError) {
      updateErrorMessage.value = '案件已被其他用户修改，请重新加载后再归档或恢复。'
    } else if (error instanceof CaseUpdateNotFoundError) {
      updateErrorMessage.value = '案件已不存在，无法更改归档状态。'
    } else {
      updateErrorMessage.value = '归档状态更新失败，请确认后端服务可用后重试。'
    }
  } finally {
    isChangingArchiveState.value = false
  }
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
        <Dialog
          v-if="caseData && !isEditing"
          v-model:open="archiveDialogOpen"
        >
          <DialogTrigger as-child>
            <Button
              :variant="caseData.archived ? 'outline' : 'destructive'"
              size="sm"
            >
              <SafeIcon
                :name="caseData.archived ? 'ArchiveRestore' : 'Archive'"
                :size="16"
                class="mr-2"
              />
              {{ caseData.archived ? '恢复案件' : '归档案件' }}
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>
                {{ caseData.archived ? '确认恢复案件' : '确认归档案件' }}
              </DialogTitle>
              <DialogDescription>
                {{
                  caseData.archived
                    ? '恢复后，案件将重新出现在默认的在办案件列表中。'
                    : '归档后，案件将从默认列表中隐藏，但可以在已归档案件中恢复。'
                }}
              </DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <Button
                variant="outline"
                :disabled="isChangingArchiveState"
                @click="archiveDialogOpen = false"
              >
                取消
              </Button>
              <Button
                :variant="caseData.archived ? 'default' : 'destructive'"
                :disabled="isChangingArchiveState"
                @click="handleArchiveStateChange"
              >
                {{ isChangingArchiveState ? '处理中...' : (caseData.archived ? '恢复' : '归档') }}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
        <template v-else-if="caseData && isEditing">
          <Button
            variant="default"
            size="sm"
            :disabled="isSubmitting"
            @click="handleSave"
          >
            <SafeIcon
              :name="isSubmitting ? 'Loader2' : 'Check'"
              :size="16"
              :class="isSubmitting ? 'mr-2 animate-spin' : 'mr-2'"
            />
            {{ isSubmitting ? '保存中...' : '保存' }}
          </Button>
          <Button
            variant="outline"
            size="sm"
            :disabled="isSubmitting"
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
        <div
          v-if="updateErrorMessage"
          role="alert"
          class="rounded-md border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive"
        >
          {{ updateErrorMessage }}
        </div>

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
              :case-id="caseData.id"
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
