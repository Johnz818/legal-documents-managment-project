
<script setup lang="ts">
import { ref } from 'vue'
import { Card, CardContent } from '@/components/ui/card'
import SafeIcon from '@/components/common/SafeIcon.vue'

interface Props {
  isLoading?: boolean
}

withDefaults(defineProps<Props>(), {
  isLoading: false,
})

const emit = defineEmits<{
  fileSelected: [file: File]
}>()

const isDragging = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const handleDragOver = (e: DragEvent) => {
  e.preventDefault()
  isDragging.value = true
}

const handleDragLeave = () => {
  isDragging.value = false
}

const handleDrop = (e: DragEvent) => {
  e.preventDefault()
  isDragging.value = false

  const files = e.dataTransfer?.files
  if (files && files.length > 0) {
    const file = files[0]
    if (isValidFile(file)) {
      emit('fileSelected', file)
    }
  }
}

const handleFileInputChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (files && files.length > 0) {
    const file = files[0]
    if (isValidFile(file)) {
      emit('fileSelected', file)
    }
  }
}

const isValidFile = (file: File): boolean => {
  const validTypes = [
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/vnd.ms-excel',
  ]
  return validTypes.includes(file.type) || file.name.endsWith('.xlsx') || file.name.endsWith('.xls')
}

const handleClickUpload = () => {
  fileInput.value?.click()
}
</script>

<template>
  <Card>
    <CardContent class="p-0">
      <div
        @dragover="handleDragOver"
        @dragleave="handleDragLeave"
        @drop="handleDrop"
        :class="[
          'border-2 border-dashed rounded-lg p-12 text-center transition-colors cursor-pointer',
          isDragging ? 'border-primary bg-primary/5' : 'border-muted-foreground/25 hover:border-primary/50',
          isLoading && 'opacity-50 cursor-not-allowed'
        ]"
        @click="handleClickUpload"
      >
        <input
          ref="fileInput"
          type="file"
          accept=".xlsx,.xls"
          class="hidden"
          @change="handleFileInputChange"
          :disabled="isLoading"
        />

        <div class="space-y-3">
          <div class="flex justify-center">
            <div class="rounded-full bg-primary/10 p-4">
<SafeIcon 
                name="Upload" 
                :size="32"
                class="text-primary"
              />
            </div>
          </div>

          <div>
            <p class="text-lg font-semibold">拖拽Excel文件到此处</p>
            <p class="text-sm text-muted-foreground mt-1">或点击选择文件</p>
          </div>

          <p class="text-xs text-muted-foreground">
            支持格式：.xlsx, .xls | 最大文件大小：10MB
          </p>
        </div>
      </div>
    </CardContent>
  </Card>
</template>
