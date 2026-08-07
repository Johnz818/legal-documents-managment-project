<script setup lang="ts">
import { Button } from '@/components/ui/button'
import SafeIcon from '@/components/common/SafeIcon.vue'
import type { GeneratedDocument } from '@/types/documentGeneration'

defineProps<{
  result: GeneratedDocument
  isDownloading: boolean
  downloadError: string
}>()

const emit = defineEmits<{
  download: []
  generateAnother: []
}>()

const formatGeneratedAt = (value: string) => new Date(value).toLocaleString('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
})
</script>

<template>
  <div class="space-y-4 rounded-lg border border-green-200 bg-green-50 p-5">
    <div class="flex items-start gap-3">
      <SafeIcon name="CircleCheck" :size="22" class="mt-0.5 text-green-700" />
      <div>
        <h3 class="font-semibold text-green-900">文书生成成功</h3>
        <p class="mt-1 text-sm text-green-800">{{ result.fileName }}</p>
        <p class="mt-1 text-xs text-green-700">生成于 {{ formatGeneratedAt(result.createdAt) }}</p>
        <p class="mt-1 text-xs text-green-700">
          生成记录 #{{ result.generationId }}<template v-if="result.caseDocumentId !== null"> · 案件文书 #{{ result.caseDocumentId }}</template> · 模板版本 {{ result.versionNumber }}
        </p>
      </div>
    </div>

    <p v-if="!result.outputAvailable" role="alert" class="rounded-md bg-amber-100 p-3 text-sm text-amber-900">
      生成记录仍然存在，但对应的案件文书已被移除，当前无法下载。
    </p>
    <p v-if="downloadError" role="alert" class="text-sm text-destructive">{{ downloadError }}</p>

    <div class="flex flex-wrap gap-2">
      <Button :disabled="!result.outputAvailable || isDownloading" @click="emit('download')">
        <SafeIcon :name="isDownloading ? 'LoaderCircle' : 'Download'" :size="16" :class="isDownloading ? 'mr-2 animate-spin' : 'mr-2'" />
        {{ isDownloading ? '下载中...' : '下载 DOCX' }}
      </Button>
      <Button variant="outline" @click="emit('generateAnother')">使用当前配置再生成一份</Button>
    </div>
  </div>
</template>
