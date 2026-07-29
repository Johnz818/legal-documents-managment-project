<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyTitle,
} from '@/components/ui/empty'
import SafeIcon from '@/components/common/SafeIcon.vue'
import {
  DocumentRemovalError,
  DocumentUploadError,
  downloadCaseDocument,
  getCaseDocuments,
  removeCaseDocument,
  uploadCaseDocument,
} from '@/services/documentService'
import type { CaseDocumentSummaryResponse } from '@/types/document'

interface Props {
  caseId: number
}

const props = defineProps<Props>()

const documents = ref<CaseDocumentSummaryResponse[]>([])
const isLoading = ref(true)
const isUploading = ref(false)
const downloadingDocumentId = ref<number | null>(null)
const removingDocumentId = ref<number | null>(null)
const documentPendingRemoval = ref<CaseDocumentSummaryResponse | null>(null)
const removalDialogOpen = ref(false)
const listErrorMessage = ref('')
const actionErrorMessage = ref('')
const fileInput = ref<HTMLInputElement | null>(null)

const loadDocuments = async () => {
  isLoading.value = true
  listErrorMessage.value = ''

  try {
    documents.value = await getCaseDocuments(props.caseId)
  } catch {
    listErrorMessage.value = '案件文件加载失败，请确认后端服务可用后重试。'
  } finally {
    isLoading.value = false
  }
}

onMounted(loadDocuments)

const chooseFile = () => {
  fileInput.value?.click()
}

const uploadErrorMessage = (error: unknown) => {
  if (!(error instanceof DocumentUploadError)) {
    return '文件上传失败，请确认后端服务可用后重试。'
  }

  const messages = {
    invalid: '文件为空或文件名无效，请重新选择。',
    'not-found': '案件已不存在，无法上传文件。',
    'too-large': '文件不能超过 5 MB。',
    unsupported: '仅支持内容有效且不含宏的 PDF、DOC 或 DOCX 文件。',
    unexpected: '文件上传失败，请确认后端服务可用后重试。',
  }
  return messages[error.reason]
}

const handleFileSelected = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }

  isUploading.value = true
  actionErrorMessage.value = ''
  try {
    await uploadCaseDocument(props.caseId, file)
    await loadDocuments()
  } catch (error) {
    actionErrorMessage.value = uploadErrorMessage(error)
  } finally {
    isUploading.value = false
    input.value = ''
  }
}

const handleDownload = async (document: CaseDocumentSummaryResponse) => {
  downloadingDocumentId.value = document.id
  actionErrorMessage.value = ''

  try {
    const content = await downloadCaseDocument(props.caseId, document.id)
    const objectUrl = URL.createObjectURL(content)
    const link = window.document.createElement('a')
    link.href = objectUrl
    link.download = document.originalFileName
    link.click()
    URL.revokeObjectURL(objectUrl)
  } catch {
    actionErrorMessage.value = '文件下载失败，请稍后重试。'
  } finally {
    downloadingDocumentId.value = null
  }
}

const requestRemoval = (document: CaseDocumentSummaryResponse) => {
  documentPendingRemoval.value = document
  removalDialogOpen.value = true
}

const removalErrorMessage = (error: unknown) => {
  if (error instanceof DocumentRemovalError && error.reason === 'not-found') {
    return '文件已不存在或不属于当前案件，请重新加载。'
  }
  return '文件移除失败，请稍后重试。'
}

const confirmRemoval = async () => {
  const document = documentPendingRemoval.value
  if (!document) {
    return
  }

  removingDocumentId.value = document.id
  actionErrorMessage.value = ''
  try {
    await removeCaseDocument(props.caseId, document.id)
    removalDialogOpen.value = false
    documentPendingRemoval.value = null
    await loadDocuments()
  } catch (error) {
    actionErrorMessage.value = removalErrorMessage(error)
    removalDialogOpen.value = false
    documentPendingRemoval.value = null
  } finally {
    removingDocumentId.value = null
  }
}

const cancelRemoval = () => {
  removalDialogOpen.value = false
  documentPendingRemoval.value = null
}

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) {
    return `${bytes} B`
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

const formatDateTime = (value: string) => {
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<template>
  <Card>
    <CardHeader class="flex flex-row items-center justify-between gap-4">
      <CardTitle>案件文件</CardTitle>
      <div>
        <input
          ref="fileInput"
          type="file"
          class="hidden"
          accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
          :disabled="isUploading"
          @change="handleFileSelected"
        >
        <Button
          size="sm"
          :disabled="isUploading"
          @click="chooseFile"
        >
          <SafeIcon
            :name="isUploading ? 'Loader2' : 'Upload'"
            :size="16"
            :class="isUploading ? 'mr-2 animate-spin' : 'mr-2'"
          />
          {{ isUploading ? '上传中...' : '上传文件' }}
        </Button>
      </div>
    </CardHeader>
    <CardContent>
      <div
        v-if="actionErrorMessage"
        role="alert"
        class="mb-4 rounded-md border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive"
      >
        {{ actionErrorMessage }}
      </div>

      <div v-if="isLoading" class="flex items-center justify-center gap-2 py-12 text-sm text-muted-foreground">
        <SafeIcon name="LoaderCircle" :size="18" class="animate-spin" />
        正在加载案件文件...
      </div>

      <div
        v-else-if="listErrorMessage"
        class="flex flex-col items-center gap-3 py-12 text-center"
      >
        <SafeIcon name="CircleAlert" :size="28" class="text-destructive" />
        <p class="text-sm text-muted-foreground">{{ listErrorMessage }}</p>
        <Button variant="outline" size="sm" @click="loadDocuments">
          重新加载
        </Button>
      </div>

      <div v-else-if="documents.length > 0" class="space-y-4">
        <div
          v-for="document in documents"
          :key="document.id"
          class="flex items-center justify-between gap-4 rounded-lg border p-4 transition-colors hover:bg-muted/50"
        >
          <div class="flex min-w-0 flex-1 items-center gap-3">
            <SafeIcon name="FileText" :size="20" class="shrink-0 text-muted-foreground" />
            <div class="min-w-0 flex-1">
              <p class="truncate font-medium">{{ document.originalFileName }}</p>
              <div class="mt-1 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                <Badge variant="outline">{{ document.fileFormat }}</Badge>
                <span>{{ formatFileSize(document.fileSize) }}</span>
                <span>{{ document.documentSource === 'UPLOADED' ? '上传于' : '生成于' }} {{ formatDateTime(document.createdAt) }}</span>
              </div>
            </div>
          </div>
          <div class="flex shrink-0 items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              :disabled="downloadingDocumentId === document.id || removingDocumentId === document.id"
              @click="handleDownload(document)"
            >
              <SafeIcon
                :name="downloadingDocumentId === document.id ? 'Loader2' : 'Download'"
                :size="16"
                :class="downloadingDocumentId === document.id ? 'mr-2 animate-spin' : 'mr-2'"
              />
              {{ downloadingDocumentId === document.id ? '下载中...' : '下载' }}
            </Button>
            <Button
              variant="destructive"
              size="sm"
              :disabled="downloadingDocumentId === document.id || removingDocumentId === document.id"
              @click="requestRemoval(document)"
            >
              <SafeIcon
                :name="removingDocumentId === document.id ? 'Loader2' : 'Trash2'"
                :size="16"
                :class="removingDocumentId === document.id ? 'mr-2 animate-spin' : 'mr-2'"
              />
              {{ removingDocumentId === document.id ? '移除中...' : '移除' }}
            </Button>
          </div>
        </div>
      </div>

      <Empty v-else class="py-8">
        <EmptyHeader>
          <div class="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-muted">
            <SafeIcon name="FileText" :size="32" class="text-muted-foreground" />
          </div>
          <EmptyTitle>暂无案件文件</EmptyTitle>
          <EmptyDescription>可上传 PDF 或 Word 文件，单个文件最大 5 MB。</EmptyDescription>
        </EmptyHeader>
        <EmptyContent class="mx-auto flex-row justify-center gap-2">
          <Button size="sm" :disabled="isUploading" @click="chooseFile">
            上传文件
          </Button>
          <Button
            as="a"
            href="./document-generation-entry.html"
            variant="outline"
            size="sm"
          >
            生成文书
          </Button>
        </EmptyContent>
      </Empty>
    </CardContent>
  </Card>

  <Dialog v-model:open="removalDialogOpen">
    <DialogContent>
      <DialogHeader>
        <DialogTitle>永久移除案件文件</DialogTitle>
        <DialogDescription>
          确定永久移除“{{ documentPendingRemoval?.originalFileName }}”吗？文件及其存储内容将被永久删除，完成后无法恢复。
        </DialogDescription>
      </DialogHeader>
      <DialogFooter>
        <Button
          variant="outline"
          :disabled="removingDocumentId !== null"
          @click="cancelRemoval"
        >
          取消
        </Button>
        <Button
          variant="destructive"
          :disabled="removingDocumentId !== null"
          @click="confirmRemoval"
        >
          {{ removingDocumentId !== null ? '移除中...' : '确认移除' }}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
