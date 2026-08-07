<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import SafeIcon from '@/components/common/SafeIcon.vue'
import CaseSelector from './CaseSelector.vue'
import GenerationResult from './GenerationResult.vue'
import GenerationValueReview from './GenerationValueReview.vue'
import TemplateSelector from './TemplateSelector.vue'
import { getCases } from '@/services/caseService'
import {
  GenerationValidationError,
  classifyGenerationFailure,
  createGenerationAttempt,
  downloadGeneratedDocument,
  getDocumentTemplateVersion,
  getDocumentTemplateVersions,
  getDocumentTemplates,
  presentGenerationError,
  prepareDocumentGeneration,
  submitGenerationAttempt,
  validateGenerationValues,
} from '@/services/documentGenerationService'
import type { CaseSummaryResponse } from '@/types/case'
import type {
  DocumentTemplateSummary,
  DocumentTemplateVersionSummary,
  DocumentFieldSource,
  GeneratedDocument,
  GenerationAttempt,
  GenerationContext,
  GenerationErrorPresentation,
  GenerationPreparation,
  GenerationValidationResult,
  PublishedTemplateVersion,
  StaleValueConflict,
} from '@/types/documentGeneration'

const PAGE_SIZE = 20

const cases = ref<CaseSummaryResponse[]>([])
const templates = ref<DocumentTemplateSummary[]>([])
const versions = ref<DocumentTemplateVersionSummary[]>([])
const exactVersion = ref<PublishedTemplateVersion | null>(null)
const preparation = ref<GenerationPreparation | null>(null)
const reviewedValues = ref<Record<string, string>>({})
const reviewedBaseline = ref<Record<string, string>>({})
const validation = ref<GenerationValidationResult>({ valid: true, fieldErrors: {} })
const errorPresentation = ref<GenerationErrorPresentation | null>(null)
const staleConflicts = ref<StaleValueConflict[]>([])
const staleRefreshSnapshot = ref<{
  preparation: GenerationPreparation
  reviewedValues: Record<string, string>
  reportedFieldKeys: string[]
} | null>(null)
const attempt = ref<GenerationAttempt | null>(null)
const result = ref<GeneratedDocument | null>(null)

const selectedCaseId = ref<number | null>(null)
const selectedTemplateId = ref<number | null>(null)
const selectedVersionNumber = ref<number | null>(null)
const timezone = ref('')

const templatePage = ref(-1)
const templateTotalPages = ref(0)
const versionPage = ref(-1)
const versionTotalPages = ref(0)
const versionRequestId = ref(0)
const exactVersionRequestId = ref(0)
const preparationRequestId = ref(0)

const isLoadingCases = ref(false)
const isLoadingTemplates = ref(false)
const isLoadingVersions = ref(false)
const isLoadingExactVersion = ref(false)
const isPreparing = ref(false)
const isGenerating = ref(false)
const isRetryState = ref(false)
const isDownloading = ref(false)
const isRefreshingStale = ref(false)

const caseError = ref('')
const templateError = ref('')
const versionError = ref('')
const downloadError = ref('')

const selectedCase = computed(() => cases.value.find(item => item.id === selectedCaseId.value) ?? null)
const hasMoreTemplates = computed(() => templatePage.value + 1 < templateTotalPages.value)
const hasMoreVersions = computed(() => versionPage.value + 1 < versionTotalPages.value)
const canPrepare = computed(() => selectedCaseId.value !== null
  && selectedTemplateId.value !== null
  && selectedVersionNumber.value !== null
  && exactVersion.value !== null
  && timezone.value.length > 0
  && !isPreparing.value)
const hasReviewedChanges = computed(() => {
  const keys = new Set([
    ...Object.keys(reviewedBaseline.value),
    ...Object.keys(reviewedValues.value),
  ])
  return [...keys].some(key => reviewedBaseline.value[key] !== reviewedValues.value[key])
})
const formLocked = computed(() => isGenerating.value
  || isRetryState.value
  || staleRefreshSnapshot.value !== null)
const unresolvedStaleConflicts = computed(() => staleConflicts.value.filter(
  conflict => conflict.resolution === null,
))
const displayedFieldErrors = computed(() => {
  const errors = { ...validation.value.fieldErrors }
  errorPresentation.value?.affectedFields.forEach(field => {
    errors[field.fieldKey] = field.message
  })
  unresolvedStaleConflicts.value.forEach(conflict => {
    errors[conflict.fieldKey] = '案件或系统建议值已更新，请确认使用哪个值。'
  })
  return errors
})

const resetGeneratedState = () => {
  attempt.value = null
  isRetryState.value = false
  result.value = null
  errorPresentation.value = null
  downloadError.value = ''
  staleConflicts.value = []
  staleRefreshSnapshot.value = null
  isRefreshingStale.value = false
}

const clearPreparation = () => {
  preparationRequestId.value += 1
  isPreparing.value = false
  preparation.value = null
  reviewedValues.value = {}
  reviewedBaseline.value = {}
  validation.value = { valid: true, fieldErrors: {} }
  resetGeneratedState()
}

const detectTimezone = () => {
  try {
    timezone.value = Intl.DateTimeFormat().resolvedOptions().timeZone || ''
  } catch {
    timezone.value = ''
  }
}

const loadCases = async () => {
  isLoadingCases.value = true
  caseError.value = ''
  try {
    cases.value = await getCases()
    const queryId = Number(new URLSearchParams(window.location.search).get('caseId'))
    if (Number.isInteger(queryId) && cases.value.some(item => item.id === queryId)) {
      selectedCaseId.value = queryId
    }
  } catch {
    caseError.value = '案件数据加载失败，请确认后端服务可用后重试。'
  } finally {
    isLoadingCases.value = false
  }
}

const appendUniqueTemplates = (items: DocumentTemplateSummary[]) => {
  const known = new Set(templates.value.map(item => item.id))
  templates.value.push(...items.filter(item => !known.has(item.id)))
}

const loadTemplates = async (reset = false) => {
  if (isLoadingTemplates.value) return
  const nextPage = reset ? 0 : templatePage.value + 1
  isLoadingTemplates.value = true
  templateError.value = ''
  if (reset) {
    templates.value = []
    templatePage.value = -1
    templateTotalPages.value = 0
  }
  try {
    const page = await getDocumentTemplates(nextPage, PAGE_SIZE)
    appendUniqueTemplates(page.items)
    templatePage.value = page.page
    templateTotalPages.value = page.totalPages
  } catch {
    templateError.value = '模板加载失败，请确认后端服务可用后重试。'
  } finally {
    isLoadingTemplates.value = false
  }
}

const appendUniqueVersions = (items: DocumentTemplateVersionSummary[]) => {
  const known = new Set(versions.value.map(item => item.versionNumber))
  versions.value.push(...items.filter(item => !known.has(item.versionNumber)))
}

const loadVersions = async (reset = false) => {
  const templateId = selectedTemplateId.value
  if (templateId === null || isLoadingVersions.value) return
  const requestId = versionRequestId.value
  const nextPage = reset ? 0 : versionPage.value + 1
  isLoadingVersions.value = true
  versionError.value = ''
  if (reset) {
    versions.value = []
    versionPage.value = -1
    versionTotalPages.value = 0
  }
  try {
    const page = await getDocumentTemplateVersions(templateId, nextPage, PAGE_SIZE)
    if (selectedTemplateId.value !== templateId || versionRequestId.value !== requestId) return
    appendUniqueVersions(page.items)
    versionPage.value = page.page
    versionTotalPages.value = page.totalPages
  } catch {
    if (selectedTemplateId.value === templateId && versionRequestId.value === requestId) {
      versionError.value = '模板版本加载失败，请重试。'
    }
  } finally {
    if (versionRequestId.value === requestId) isLoadingVersions.value = false
  }
}

const handleCaseSelect = (caseId: number) => {
  if (formLocked.value) return
  if (selectedCaseId.value === caseId) return
  selectedCaseId.value = caseId
  clearPreparation()
}

const handleTemplateSelect = (templateId: number) => {
  if (formLocked.value) return
  if (selectedTemplateId.value === templateId) return
  selectedTemplateId.value = templateId
  versionRequestId.value += 1
  exactVersionRequestId.value += 1
  isLoadingVersions.value = false
  isLoadingExactVersion.value = false
  selectedVersionNumber.value = null
  exactVersion.value = null
  clearPreparation()
  void loadVersions(true)
}

const handleVersionSelect = async (versionNumber: number) => {
  if (formLocked.value) return
  const templateId = selectedTemplateId.value
  if (templateId === null) return
  const requestId = ++exactVersionRequestId.value
  selectedVersionNumber.value = versionNumber
  exactVersion.value = null
  clearPreparation()
  isLoadingExactVersion.value = true
  versionError.value = ''
  try {
    const loaded = await getDocumentTemplateVersion(templateId, versionNumber)
    if (exactVersionRequestId.value === requestId
        && selectedTemplateId.value === templateId
        && selectedVersionNumber.value === versionNumber) {
      exactVersion.value = loaded
    }
  } catch {
    if (exactVersionRequestId.value === requestId
        && selectedTemplateId.value === templateId
        && selectedVersionNumber.value === versionNumber) {
      versionError.value = '所选模板版本读取失败，请重新选择。'
    }
  } finally {
    if (exactVersionRequestId.value === requestId) isLoadingExactVersion.value = false
  }
}

const generationContext = (): GenerationContext | null => {
  if (selectedCaseId.value === null || selectedTemplateId.value === null
      || selectedVersionNumber.value === null || !timezone.value) return null
  return {
    caseId: selectedCaseId.value,
    templateId: selectedTemplateId.value,
    versionNumber: selectedVersionNumber.value,
    timezone: timezone.value,
  }
}

const handlePrepare = async () => {
  const context = generationContext()
  if (!context || !canPrepare.value) return
  clearPreparation()
  const requestId = preparationRequestId.value
  isPreparing.value = true
  errorPresentation.value = null
  try {
    const loaded = await prepareDocumentGeneration(context)
    if (preparationRequestId.value !== requestId
        || selectedCaseId.value !== context.caseId
        || selectedTemplateId.value !== context.templateId
        || selectedVersionNumber.value !== context.versionNumber
        || timezone.value !== context.timezone) return
    preparation.value = loaded
    reviewedValues.value = Object.fromEntries(
      loaded.fields.map(field => [field.fieldKey, field.suggestedValue ?? '']),
    )
    reviewedBaseline.value = { ...reviewedValues.value }
  } catch (error) {
    if (preparationRequestId.value === requestId) {
      errorPresentation.value = presentGenerationError(
        error,
        exactVersion.value?.fields ?? [],
        '生成信息准备失败',
      )
    }
  } finally {
    if (preparationRequestId.value === requestId) isPreparing.value = false
  }
}

const handleValueUpdate = (fieldKey: string, value: string) => {
  if (formLocked.value) return
  reviewedValues.value = { ...reviewedValues.value, [fieldKey]: value }
  validation.value = { valid: true, fieldErrors: {} }
  attempt.value = null
  result.value = null
  errorPresentation.value = errorPresentation.value
    ? {
        ...errorPresentation.value,
        affectedFields: errorPresentation.value.affectedFields.filter(
          field => field.fieldKey !== fieldKey,
        ),
      }
    : null
  const conflict = staleConflicts.value.find(item => item.fieldKey === fieldKey)
  if (conflict) {
    conflict.previousValue = value
    conflict.resolution = 'KEEP_PREVIOUS'
  }
}

const isHumanOverride = (
  field: GenerationPreparation['fields'][number],
  value: string,
) => field.status === 'REQUIRES_USER_INPUT'
  || field.defaultSource === 'USER_INPUT'
  || value !== (field.suggestedValue ?? '')

const refreshStalePreparation = async () => {
  const snapshot = staleRefreshSnapshot.value
  const context = generationContext()
  if (!snapshot || !context || isRefreshingStale.value) return
  isRefreshingStale.value = true
  try {
    const refreshed = await prepareDocumentGeneration(context)
    const oldFields = new Map(snapshot.preparation.fields.map(field => [field.fieldKey, field]))
    const reportedKeys = new Set(snapshot.reportedFieldKeys)
    const mergedValues: Record<string, string> = {}
    const conflicts: StaleValueConflict[] = []

    refreshed.fields.forEach(currentField => {
      const oldField = oldFields.get(currentField.fieldKey)
      const previousValue = snapshot.reviewedValues[currentField.fieldKey] ?? ''
      const preserveOverride = oldField && isHumanOverride(oldField, previousValue)
      mergedValues[currentField.fieldKey] = preserveOverride
        ? previousValue
        : currentField.suggestedValue ?? ''

      const deterministicBefore = oldField
        && oldField.defaultSource !== 'USER_INPUT'
        && oldField.status === 'RESOLVED'
      const suggestionChanged = deterministicBefore
        && oldField.suggestedValue !== currentField.suggestedValue
      if (currentField.defaultSource !== 'USER_INPUT'
          && (suggestionChanged || reportedKeys.has(currentField.fieldKey))) {
        conflicts.push({
          fieldKey: currentField.fieldKey,
          displayName: currentField.displayName,
          previousValue,
          currentValue: currentField.suggestedValue ?? '',
          currentSource: currentField.defaultSource,
          resolution: null,
        })
      }
    })

    preparation.value = refreshed
    reviewedValues.value = mergedValues
    reviewedBaseline.value = { ...mergedValues }
    staleConflicts.value = conflicts
    validation.value = { valid: true, fieldErrors: {} }
    staleRefreshSnapshot.value = null
  } catch (error) {
    errorPresentation.value = presentGenerationError(
      error,
      snapshot.preparation.fields,
      '当前案件和系统值刷新失败',
    )
  } finally {
    isRefreshingStale.value = false
  }
}

const applyFailure = async (error: unknown) => {
  const action = classifyGenerationFailure(error)
  const activeFields = preparation.value?.fields ?? []
  errorPresentation.value = presentGenerationError(error, activeFields)
  if (action === 'RETRY_EXACT_REQUEST') {
    isRetryState.value = true
    return
  }
  attempt.value = null
  isRetryState.value = false
  if (action === 'REFRESH_PREPARATION') {
    if (preparation.value) {
      staleRefreshSnapshot.value = {
        preparation: preparation.value,
        reviewedValues: { ...reviewedValues.value },
        reportedFieldKeys: errorPresentation.value.affectedFields.map(
          field => field.fieldKey,
        ),
      }
      await refreshStalePreparation()
    }
  } else if (action === 'RESELECT_CASE') {
    selectedCaseId.value = null
    const presentation = errorPresentation.value
    clearPreparation()
    errorPresentation.value = presentation
  } else if (action === 'RESELECT_TEMPLATE_VERSION' || action === 'STOP_TEMPLATE_INTEGRITY') {
    selectedVersionNumber.value = null
    exactVersion.value = null
    const presentation = errorPresentation.value
    clearPreparation()
    errorPresentation.value = presentation
  }
}

const submitAttempt = async (currentAttempt: GenerationAttempt) => {
  if (isGenerating.value) return
  isGenerating.value = true
  errorPresentation.value = null
  try {
    result.value = await submitGenerationAttempt(currentAttempt)
    reviewedBaseline.value = { ...reviewedValues.value }
    attempt.value = null
    isRetryState.value = false
  } catch (error) {
    await applyFailure(error)
  } finally {
    isGenerating.value = false
  }
}

const handleGenerate = () => {
  if (!preparation.value || isGenerating.value || isRetryState.value
      || unresolvedStaleConflicts.value.length > 0) return
  const context = generationContext()
  if (!context) return
  validation.value = validateGenerationValues(preparation.value.fields, reviewedValues.value)
  if (!validation.value.valid) return
  try {
    const sourceOverrides = Object.fromEntries(
      staleConflicts.value
        .filter(conflict => conflict.resolution !== null)
        .map(conflict => [
          conflict.fieldKey,
          conflict.resolution === 'USE_CURRENT' ? conflict.currentSource : 'USER_INPUT',
        ]),
    ) as Partial<Record<string, DocumentFieldSource>>
    attempt.value = createGenerationAttempt(
      context,
      preparation.value.fields,
      reviewedValues.value,
      undefined,
      sourceOverrides,
    )
    void submitAttempt(attempt.value)
  } catch (error) {
    if (error instanceof GenerationValidationError) validation.value = error.validation
  }
}

const handleRetry = () => {
  if (attempt.value && isRetryState.value) void submitAttempt(attempt.value)
}

const handleEditValues = () => {
  attempt.value = null
  isRetryState.value = false
  errorPresentation.value = null
}

const handleResolveConflict = (
  fieldKey: string,
  resolution: 'USE_CURRENT' | 'KEEP_PREVIOUS',
) => {
  const conflict = staleConflicts.value.find(item => item.fieldKey === fieldKey)
  if (!conflict) return
  conflict.resolution = resolution
  reviewedValues.value = {
    ...reviewedValues.value,
    [fieldKey]: resolution === 'USE_CURRENT'
      ? conflict.currentValue
      : conflict.previousValue,
  }
  errorPresentation.value = errorPresentation.value
    ? {
        ...errorPresentation.value,
        affectedFields: errorPresentation.value.affectedFields.filter(
          field => field.fieldKey !== fieldKey,
        ),
      }
    : null
  if (staleConflicts.value.every(item => item.resolution !== null)) {
    errorPresentation.value = null
  }
}

const handleDownload = async () => {
  if (!result.value || isDownloading.value) return
  isDownloading.value = true
  downloadError.value = ''
  try {
    const content = await downloadGeneratedDocument(result.value)
    const objectUrl = URL.createObjectURL(content)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = result.value.fileName
    link.click()
    URL.revokeObjectURL(objectUrl)
  } catch {
    downloadError.value = '文书下载失败，请稍后重试。'
  } finally {
    isDownloading.value = false
  }
}

const handleGenerateAnother = () => {
  result.value = null
  downloadError.value = ''
  errorPresentation.value = null
  staleConflicts.value = []
  reviewedBaseline.value = { ...reviewedValues.value }
}

const handleCancel = () => {
  if (isGenerating.value) return
  if (hasReviewedChanges.value
      && !window.confirm('当前审核值尚未生成，确定要放弃并返回吗？')) return
  window.location.href = './document-generation-entry.html'
}

const loadRequestedTemplateSelection = async () => {
  const params = new URLSearchParams(window.location.search)
  const templateId = Number(params.get('templateId'))
  const versionNumber = Number(params.get('versionNumber'))
  if (!Number.isSafeInteger(templateId) || templateId < 1
      || !Number.isSafeInteger(versionNumber) || versionNumber < 1) return

  const requestId = ++exactVersionRequestId.value
  selectedTemplateId.value = templateId
  selectedVersionNumber.value = versionNumber
  isLoadingExactVersion.value = true
  versionRequestId.value += 1
  try {
    const loaded = await getDocumentTemplateVersion(templateId, versionNumber)
    if (requestId !== exactVersionRequestId.value) return
    exactVersion.value = loaded
    await loadVersions(true)
  } catch {
    if (requestId !== exactVersionRequestId.value) return
    selectedTemplateId.value = null
    selectedVersionNumber.value = null
    exactVersion.value = null
    versionError.value = '链接中的模板版本不可用，请重新选择。'
  } finally {
    if (requestId === exactVersionRequestId.value) isLoadingExactVersion.value = false
  }
}

onMounted(() => {
  detectTimezone()
  void loadCases()
  void loadTemplates(true)
  void loadRequestedTemplateSelection()
})
</script>

<template>
  <div class="flex-1 space-y-6 p-6">
    <Alert v-if="!timezone" variant="destructive">
      <SafeIcon name="TriangleAlert" :size="16" class="mr-2" />
      <AlertDescription>无法读取浏览器的 IANA 时区，当前不能准备系统日期。请检查浏览器设置后刷新页面。</AlertDescription>
    </Alert>

    <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
      <Card>
        <CardHeader>
          <CardTitle class="text-lg">1. 选择案件</CardTitle>
          <CardDescription>选择生成文书所归属的案件</CardDescription>
        </CardHeader>
        <CardContent>
          <CaseSelector
            :cases="cases"
            :selected-case-id="selectedCaseId"
            :is-loading="isLoadingCases"
            :error-message="caseError"
            :disabled="formLocked"
            @select="handleCaseSelect"
            @retry="loadCases"
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle class="text-lg">2. 选择模板和版本</CardTitle>
          <CardDescription>明确选择一个不可变的已发布版本</CardDescription>
        </CardHeader>
        <CardContent>
          <TemplateSelector
            :templates="templates"
            :versions="versions"
            :selected-template-id="selectedTemplateId"
            :selected-version-number="selectedVersionNumber"
            :exact-version="exactVersion"
            :is-loading-templates="isLoadingTemplates"
            :is-loading-versions="isLoadingVersions"
            :is-loading-exact-version="isLoadingExactVersion"
            :template-error="templateError"
            :version-error="versionError"
            :has-more-templates="hasMoreTemplates"
            :has-more-versions="hasMoreVersions"
            :disabled="formLocked"
            @select-template="handleTemplateSelect"
            @select-version="handleVersionSelect"
            @load-more-templates="loadTemplates(false)"
            @load-more-versions="loadVersions(false)"
            @retry-templates="loadTemplates(templatePage < 0)"
            @retry-versions="loadVersions(versionPage < 0)"
          />
        </CardContent>
      </Card>
    </div>

    <Card>
      <CardHeader>
        <CardTitle class="text-lg">3. 准备并审核字段值</CardTitle>
        <CardDescription>
          <span v-if="selectedCase">案件：{{ selectedCase.caseNumber }} · </span>
          时区：{{ timezone || '不可用' }}
        </CardDescription>
      </CardHeader>
      <CardContent class="space-y-4">
        <Button v-if="!preparation" :disabled="!canPrepare" @click="handlePrepare">
          <SafeIcon :name="isPreparing ? 'LoaderCircle' : 'ListChecks'" :size="16" :class="isPreparing ? 'mr-2 animate-spin' : 'mr-2'" />
          {{ isPreparing ? '准备中...' : '准备字段值' }}
        </Button>

        <GenerationValueReview
          v-if="preparation"
          :fields="preparation.fields"
          :values="reviewedValues"
          :field-errors="displayedFieldErrors"
          :form-error="validation.formError"
          :disabled="formLocked"
          :conflicts="staleConflicts"
          @update-value="handleValueUpdate"
          @resolve-conflict="handleResolveConflict"
        />

        <div v-if="errorPresentation" role="alert" class="space-y-2 rounded-md border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive">
          <p class="font-medium">{{ errorPresentation.summary }}</p>
          <p v-if="errorPresentation.detail">{{ errorPresentation.detail }}</p>
          <ul v-if="errorPresentation.affectedFields.length" class="list-disc space-y-1 pl-5">
            <li v-for="field in errorPresentation.affectedFields" :key="field.fieldKey">
              受影响字段：{{ field.displayName }}（{{ field.fieldKey }}）
            </li>
          </ul>
          <Button
            v-if="staleRefreshSnapshot"
            variant="outline"
            size="sm"
            :disabled="isRefreshingStale"
            @click="refreshStalePreparation"
          >
            {{ isRefreshingStale ? '刷新中...' : '重试刷新当前值' }}
          </Button>
        </div>

        <div v-if="preparation && !result" class="flex flex-wrap justify-between gap-2">
          <Button variant="ghost" :disabled="isGenerating" @click="handleCancel">取消生成</Button>
          <div class="flex flex-wrap justify-end gap-2">
          <template v-if="isRetryState">
            <Button variant="outline" :disabled="isGenerating" @click="handleEditValues">返回编辑字段</Button>
            <Button :disabled="isGenerating" @click="handleRetry">
              <SafeIcon :name="isGenerating ? 'LoaderCircle' : 'RefreshCw'" :size="16" :class="isGenerating ? 'mr-2 animate-spin' : 'mr-2'" />
              {{ isGenerating ? '重试中...' : '重试同一请求' }}
            </Button>
          </template>
          <Button
            v-else
            :disabled="isGenerating || isRefreshingStale || unresolvedStaleConflicts.length > 0"
            @click="handleGenerate"
          >
            <SafeIcon :name="isGenerating ? 'LoaderCircle' : 'FileCheck2'" :size="16" :class="isGenerating ? 'mr-2 animate-spin' : 'mr-2'" />
            {{ isGenerating ? '生成中...' : '确认字段并生成 DOCX' }}
          </Button>
          </div>
        </div>
      </CardContent>
    </Card>

    <GenerationResult
      v-if="result"
      :result="result"
      :is-downloading="isDownloading"
      :download-error="downloadError"
      @download="handleDownload"
      @generate-another="handleGenerateAnother"
    />
  </div>
</template>
