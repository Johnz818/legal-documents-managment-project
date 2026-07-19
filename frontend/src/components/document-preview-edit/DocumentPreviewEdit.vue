
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Alert, AlertDescription } from '@/components/ui/alert'
import SafeIcon from '@/components/common/SafeIcon.vue'
import DocumentHeader from './DocumentHeader.vue'
import DocumentEditor from './DocumentEditor.vue'
import DocumentExportPanel from './DocumentExportPanel.vue'
import { generateDocumentPreview } from '@/data/document'
import { getCaseById } from '@/data/case'
import { MOCK_USERS } from '@/data/user'

// Mock data - 从查询参数或默认值获取
const defaultCaseId = 'C2024001'
const defaultTemplateId = 'T001'

const documentContent = ref('')
const isSaving = ref(false)
const isExporting = ref(false)
const exportFormat = ref<'word' | 'pdf'>('word')
const activeTab = ref('preview')
const saveSuccess = ref(false)

// 初始化文书数据
const documentData = computed(() => {
  const doc = generateDocumentPreview(defaultCaseId, defaultTemplateId)
  if (doc) {
    documentContent.value = doc.content
    return doc
  }
  return null
})

const caseData = computed(() => {
  return getCaseById(defaultCaseId)
})

const leadAttorney = computed(() => {
  if (!caseData.value) return null
  return MOCK_USERS.find(u => u.id === caseData.value?.leadAttorneyId)
})

// 保存文书
const handleSave = async () => {
  isSaving.value = true
  try {
    // 模拟保存延迟
    await new Promise(resolve => setTimeout(resolve, 800))
    saveSuccess.value = true
    setTimeout(() => {
      saveSuccess.value = false
    }, 3000)
  } finally {
    isSaving.value = false
  }
}

// 导出文书
const handleExport = async (format: 'word' | 'pdf') => {
  isExporting.value = true
  exportFormat.value = format
  try {
    // 模拟导出延迟
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    // 模拟下载
    const element = document.createElement('a')
    const file = new Blob([documentContent.value], { type: 'text/plain' })
    element.href = URL.createObjectURL(file)
    element.download = `${documentData.value?.docName || '文书'}.${format === 'word' ? 'docx' : 'pdf'}`
    document.body.appendChild(element)
    element.click()
    document.body.removeChild(element)
  } finally {
    isExporting.value = false
  }
}

// 返回上一步
const handleGoBack = () => {
  if (typeof window !== 'undefined') {
    window.history.back()
  }
}

// 查看案件详情
const handleViewCase = () => {
  if (typeof window !== 'undefined') {
    window.location.href = `./case-detail-view.html?id=${defaultCaseId}`
  }
}
</script>

<template>
  <div class="flex flex-col h-full">
    <!-- 顶部操作栏 -->
    <div class="border-b bg-background px-6 py-4">
      <div class="flex items-center justify-between gap-4">
        <div class="flex items-center gap-2">
          <Button variant="ghost" size="icon" @click="handleGoBack">
            <SafeIcon name="ArrowLeft" :size="20" />
          </Button>
          <div>
            <h1 class="text-xl font-semibold">文书预览与编辑</h1>
            <p class="text-sm text-muted-foreground">
              {{ documentData?.docName || '文书' }}
            </p>
          </div>
        </div>

        <div class="flex items-center gap-2">
          <Button
            variant="outline"
            @click="handleExport('word')"
            :disabled="isExporting"
          >
            <SafeIcon name="Download" :size="16" class="mr-2" />
            {{ isExporting && exportFormat === 'word' ? '导出中...' : '导出 Word' }}
          </Button>
          <Button
            variant="outline"
            @click="handleExport('pdf')"
            :disabled="isExporting"
          >
            <SafeIcon name="Download" :size="16" class="mr-2" />
            {{ isExporting && exportFormat === 'pdf' ? '导出中...' : '导出 PDF' }}
          </Button>
          <Button
            @click="handleSave"
            :disabled="isSaving"
          >
            <SafeIcon name="Save" :size="16" class="mr-2" />
            {{ isSaving ? '保存中...' : '保存文书' }}
          </Button>
        </div>
      </div>

      <!-- 保存成功提示 -->
      <Alert v-if="saveSuccess" class="mt-4 border-green-200 bg-green-50">
        <SafeIcon name="CheckCircle" :size="16" class="text-green-600" />
        <AlertDescription class="text-green-800">
          文书已成功保存
        </AlertDescription>
      </Alert>
    </div>

    <!-- 主内容区域 -->
    <div class="flex-1 overflow-hidden">
      <Tabs v-model="activeTab" class="h-full flex flex-col">
        <TabsList class="w-full justify-start rounded-none border-b bg-background px-6 py-0">
          <TabsTrigger value="preview" class="rounded-none border-b-2 border-transparent data-[state=active]:border-primary">
            <SafeIcon name="Eye" :size="16" class="mr-2" />
            预览
          </TabsTrigger>
          <TabsTrigger value="edit" class="rounded-none border-b-2 border-transparent data-[state=active]:border-primary">
            <SafeIcon name="Edit" :size="16" class="mr-2" />
            编辑
          </TabsTrigger>
          <TabsTrigger value="info" class="rounded-none border-b-2 border-transparent data-[state=active]:border-primary">
            <SafeIcon name="Info" :size="16" class="mr-2" />
            详情
          </TabsTrigger>
        </TabsList>

        <!-- 预览标签页 -->
        <TabsContent value="preview" class="flex-1 overflow-auto">
          <div class="p-6">
            <Card>
              <CardContent class="p-8">
                <div class="prose prose-sm max-w-none whitespace-pre-wrap text-sm leading-relaxed">
                  {{ documentContent }}
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <!-- 编辑标签页 -->
        <TabsContent value="edit" class="flex-1 overflow-hidden">
          <DocumentEditor v-model="documentContent" />
        </TabsContent>

        <!-- 详情标签页 -->
        <TabsContent value="info" class="flex-1 overflow-auto">
          <div class="p-6 space-y-6">
            <!-- 文书信息 -->
            <DocumentHeader
              v-if="documentData && caseData"
              :document="documentData"
              :case="caseData"
              :lead-attorney="leadAttorney"
            />

            <!-- 导出选项 -->
            <DocumentExportPanel
              :is-exporting="isExporting"
              :export-format="exportFormat"
              @export="handleExport"
            />

            <!-- 操作按钮 -->
            <Card>
              <CardHeader>
                <CardTitle class="text-base">操作</CardTitle>
              </CardHeader>
              <CardContent class="space-y-2">
                <Button
                  variant="outline"
                  class="w-full justify-start"
                  @click="handleViewCase"
                >
                  <SafeIcon name="FolderOpen" :size="16" class="mr-2" />
                  查看案件详情
                </Button>
                <Button
                  variant="outline"
                  class="w-full justify-start"
                  @click="handleGoBack"
                >
                  <SafeIcon name="ArrowLeft" :size="16" class="mr-2" />
                  返回上一步
                </Button>
              </CardContent>
            </Card>
          </div>
        </TabsContent>
      </Tabs>
    </div>
  </div>
</template>

<style scoped>
:deep(.prose) {
  font-family: 'SimSun', 'Microsoft YaHei', sans-serif;
  line-height: 1.8;
}

:deep(.prose p) {
  margin: 0.5em 0;
  text-indent: 2em;
}

:deep(.prose p:first-child) {
  text-indent: 0;
}
</style>
