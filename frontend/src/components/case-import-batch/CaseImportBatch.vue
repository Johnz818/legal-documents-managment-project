
<script setup lang="ts">
import { ref, computed } from 'vue'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'
import TemplateDownloadCard from './TemplateDownloadCard.vue'
import FileUploadArea from './FileUploadArea.vue'
import ImportResultDisplay from './ImportResultDisplay.vue'
import { IMPORT_TEMPLATE_INFO, simulateCaseImport, type ImportResultModel } from '@/data/case'

type ImportStep = 'download' | 'upload' | 'result'

const currentStep = ref<ImportStep>('download')
const selectedFile = ref<File | null>(null)
const isImporting = ref(false)
const importResult = ref<ImportResultModel | null>(null)

const canProceedToUpload = computed(() => {
  return currentStep.value === 'download'
})

const canProceedToResult = computed(() => {
  return selectedFile.value !== null && !isImporting.value
})

const handleFileSelected = (file: File) => {
  selectedFile.value = file
}

const handleStartImport = async () => {
  if (!selectedFile.value) return

  isImporting.value = true
  
  // Simulate import delay
  await new Promise(resolve => setTimeout(resolve, 1500))
  
  importResult.value = simulateCaseImport(selectedFile.value)
  isImporting.value = false
  currentStep.value = 'result'
}

const handleBackToUpload = () => {
  currentStep.value = 'upload'
  selectedFile.value = null
  importResult.value = null
}

const handleReturnToCaseList = () => {
  if (typeof window !== 'undefined') {
    window.location.href = './case-list-view.html'
  }
}

const handleDownloadTemplate = () => {
  // Simulate template download
  if (typeof window !== 'undefined') {
    const link = document.createElement('a')
    link.href = IMPORT_TEMPLATE_INFO.downloadUrl
    link.download = 'case-import-template.xlsx'
    link.click()
  }
}

const handleProceedToUpload = () => {
  currentStep.value = 'upload'
}
</script>

<template>
  <div class="max-w-4xl mx-auto space-y-6">
    <!-- Progress Indicator -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div 
          class="flex h-10 w-10 items-center justify-center rounded-full"
          :class="currentStep === 'download' || ['upload', 'result'].includes(currentStep) ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground'"
        >
          <span class="text-sm font-semibold">1</span>
        </div>
        <span class="text-sm font-medium">下载模板</span>
      </div>

      <div class="flex-1 h-1 mx-4" :class="['upload', 'result'].includes(currentStep) ? 'bg-primary' : 'bg-muted'" />

      <div class="flex items-center gap-4">
        <div 
          class="flex h-10 w-10 items-center justify-center rounded-full"
          :class="currentStep === 'upload' || currentStep === 'result' ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground'"
        >
          <span class="text-sm font-semibold">2</span>
        </div>
        <span class="text-sm font-medium">上传文件</span>
      </div>

      <div class="flex-1 h-1 mx-4" :class="currentStep === 'result' ? 'bg-primary' : 'bg-muted'" />

      <div class="flex items-center gap-4">
        <div 
          class="flex h-10 w-10 items-center justify-center rounded-full"
          :class="currentStep === 'result' ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground'"
        >
          <span class="text-sm font-semibold">3</span>
        </div>
        <span class="text-sm font-medium">导入结果</span>
      </div>
    </div>

    <!-- Step 1: Download Template -->
    <div v-if="currentStep === 'download'" class="space-y-4">
      <TemplateDownloadCard 
        :template-info="IMPORT_TEMPLATE_INFO"
        @download="handleDownloadTemplate"
      />

      <Alert class="bg-blue-50 border-blue-200">
        <SafeIcon name="Info" :size="16" class="text-blue-600" />
        <AlertTitle class="text-blue-900">导入说明</AlertTitle>
        <AlertDescription class="text-blue-800">
          <ul class="list-disc list-inside space-y-1 mt-2">
            <li>请下载标准Excel模板，按照要求填写案件信息</li>
            <li>必填字段：案号、法院名称、原告、被告、案由、案件阶段</li>
            <li>案件阶段请参考枚举值：立案中、审理准备阶段、审理中、已判决(上诉期内)、上诉审理中、已判决(生效)、执行中、已归档</li>
            <li>主办律师工号需与系统中已有用户匹配</li>
            <li>日期格式统一为 YYYY-MM-DD</li>
          </ul>
        </AlertDescription>
      </Alert>

      <div class="flex justify-between">
        <Button variant="outline" as="a" href="./case-list-view.html">
          <SafeIcon name="ArrowLeft" :size="16" class="mr-2" />
          返回案件列表
        </Button>
        <Button @click="handleProceedToUpload">
          下一步：上传文件
          <SafeIcon name="ArrowRight" :size="16" class="ml-2" />
        </Button>
      </div>
    </div>

    <!-- Step 2: Upload File -->
    <div v-if="currentStep === 'upload'" class="space-y-4">
      <FileUploadArea 
        :is-loading="isImporting"
        @file-selected="handleFileSelected"
      />

      <div v-if="selectedFile" class="rounded-lg border border-green-200 bg-green-50 p-4">
        <div class="flex items-center gap-3">
          <SafeIcon name="CheckCircle" :size="20" class="text-green-600" />
          <div class="flex-1">
            <p class="font-medium text-green-900">文件已选择</p>
            <p class="text-sm text-green-700">{{ selectedFile.name }} ({{ (selectedFile.size / 1024).toFixed(2) }} KB)</p>
          </div>
        </div>
      </div>

      <div class="flex justify-between">
        <Button variant="outline" @click="() => currentStep = 'download'">
          <SafeIcon name="ArrowLeft" :size="16" class="mr-2" />
          上一步
        </Button>
        <Button 
          @click="handleStartImport"
          :disabled="!canProceedToResult || isImporting"
        >
          <SafeIcon v-if="isImporting" name="Loader" :size="16" class="mr-2 animate-spin" />
          <SafeIcon v-else name="Upload" :size="16" class="mr-2" />
          {{ isImporting ? '导入中...' : '开始导入' }}
        </Button>
      </div>
    </div>

    <!-- Step 3: Import Result -->
    <div v-if="currentStep === 'result' && importResult" class="space-y-4">
      <ImportResultDisplay :result="importResult" />

      <div class="flex justify-between">
        <Button variant="outline" @click="handleBackToUpload">
          <SafeIcon name="ArrowLeft" :size="16" class="mr-2" />
          重新上传
        </Button>
        <Button @click="handleReturnToCaseList">
          返回案件列表
          <SafeIcon name="ArrowRight" :size="16" class="ml-2" />
        </Button>
      </div>
    </div>
  </div>
</template>
