
<script setup lang="ts">
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'

interface Props {
  isExporting: boolean
  exportFormat: 'word' | 'pdf'
}

interface Emits {
  (e: 'export', format: 'word' | 'pdf'): void
}

defineProps<Props>()
defineEmits<Emits>()

const exportOptions = [
  {
    format: 'word' as const,
    label: 'Microsoft Word',
    description: '导出为 .docx 格式，可在 Word 中继续编辑',
    icon: 'FileText',
  },
  {
    format: 'pdf' as const,
    label: 'PDF 文档',
    description: '导出为 .pdf 格式，保持格式不变，便于分享和打印',
    icon: 'FileText',
  },
]
</script>

<template>
  <Card>
    <CardHeader>
      <CardTitle class="text-base">导出文书</CardTitle>
      <CardDescription>选择导出格式并下载文书</CardDescription>
    </CardHeader>
    <CardContent class="space-y-3">
      <div class="grid gap-3">
        <div
          v-for="option in exportOptions"
          :key="option.format"
          class="flex items-center justify-between p-3 border rounded-lg hover:bg-muted/50 transition-colors"
        >
          <div class="flex items-center gap-3">
            <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
              <SafeIcon :name="option.icon" :size="20" class="text-primary" />
            </div>
            <div>
              <p class="font-medium text-sm">{{ option.label }}</p>
              <p class="text-xs text-muted-foreground">{{ option.description }}</p>
            </div>
          </div>
          <Button
            size="sm"
            @click="$emit('export', option.format)"
            :disabled="isExporting"
          >
            <SafeIcon
              :name="isExporting && exportFormat === option.format ? 'Loader' : 'Download'"
              :size="16"
              class="mr-1"
            />
            {{ isExporting && exportFormat === option.format ? '导出中' : '导出' }}
          </Button>
        </div>
      </div>

      <!-- 导出说明 -->
      <div class="bg-blue-50 border border-blue-200 rounded-lg p-3 text-xs text-blue-800">
        <p class="font-medium mb-1">📋 导出说明</p>
        <ul class="space-y-1 list-disc list-inside">
          <li>导出前请确保文书内容已完整编辑</li>
          <li>Word 格式支持后续编辑和修改</li>
          <li>PDF 格式适合最终定稿和存档</li>
          <li>导出文件将自动下载到您的设备</li>
        </ul>
      </div>
    </CardContent>
  </Card>
</template>
