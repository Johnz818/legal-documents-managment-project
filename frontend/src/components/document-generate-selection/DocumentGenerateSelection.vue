
<script setup lang="ts">
import { ref, computed } from 'vue'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Alert, AlertDescription } from '@/components/ui/alert'
import SafeIcon from '@/components/common/SafeIcon.vue'
import CaseSelector from './CaseSelector.vue'
import TemplateSelector from './TemplateSelector.vue'
import { getCaseSummaryList } from '@/data/case'
import { MOCK_DOCUMENT_TEMPLATES } from '@/data/document'

// Initialize with mock data
const cases = getCaseSummaryList()
const templates = MOCK_DOCUMENT_TEMPLATES

const selectedCaseId = ref<string | null>(null)
const selectedTemplateId = ref<string | null>(null)
const isGenerating = ref(false)

const selectedCase = computed(() => {
  return cases.find(c => c.id === selectedCaseId.value)
})

const selectedTemplate = computed(() => {
  return templates.find(t => t.templateId === selectedTemplateId.value)
})

const canGenerate = computed(() => {
  return selectedCaseId.value && selectedTemplateId.value && !isGenerating.value
})

const handleCaseSelect = (caseId: string) => {
  selectedCaseId.value = caseId
}

const handleTemplateSelect = (templateId: string) => {
  selectedTemplateId.value = templateId
}

const handleGenerate = async () => {
  if (!canGenerate.value) return

  isGenerating.value = true
  
  // Simulate generation delay
  await new Promise(resolve => setTimeout(resolve, 800))
  
  // Navigate to preview page with selected case and template
  if (typeof window !== 'undefined') {
    const params = new URLSearchParams({
      caseId: selectedCaseId.value!,
      templateId: selectedTemplateId.value!,
    })
    window.location.href = `./document-preview-edit.html?${params.toString()}`
  }
}

const handleBack = () => {
  if (typeof window !== 'undefined') {
    window.history.back()
  }
}
</script>

<template>
  <div class="flex-1 p-6 space-y-6">
    <!-- Selection Steps -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <!-- Case Selection -->
      <Card class="flex flex-col">
        <CardHeader>
          <div class="flex items-center gap-2">
            <div class="flex h-8 w-8 items-center justify-center rounded-full bg-primary/10 text-primary">
              <span class="text-sm font-semibold">1</span>
            </div>
            <div>
              <CardTitle class="text-lg">选择案件</CardTitle>
              <CardDescription>选择需要生成文书的案件</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent class="flex-1">
          <CaseSelector 
            :cases="cases"
            :selected-case-id="selectedCaseId"
            @select="handleCaseSelect"
          />
        </CardContent>
      </Card>

      <!-- Template Selection -->
      <Card class="flex flex-col">
        <CardHeader>
          <div class="flex items-center gap-2">
            <div class="flex h-8 w-8 items-center justify-center rounded-full bg-primary/10 text-primary">
              <span class="text-sm font-semibold">2</span>
            </div>
            <div>
              <CardTitle class="text-lg">选择模板</CardTitle>
              <CardDescription>选择文书模板进行生成</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent class="flex-1">
          <TemplateSelector 
            :templates="templates"
            :selected-template-id="selectedTemplateId"
            @select="handleTemplateSelect"
          />
        </CardContent>
      </Card>
    </div>

    <!-- Selection Summary -->
    <Card v-if="selectedCase || selectedTemplate">
      <CardHeader>
        <CardTitle class="text-base">生成预览</CardTitle>
      </CardHeader>
      <CardContent class="space-y-4">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <!-- Selected Case Info -->
          <div v-if="selectedCase" class="space-y-2 p-4 bg-muted rounded-lg">
            <h4 class="font-semibold text-sm">选中的案件</h4>
            <div class="space-y-1 text-sm">
              <p><span class="text-muted-foreground">案号：</span>{{ selectedCase.caseNumber }}</p>
              <p><span class="text-muted-foreground">法院：</span>{{ selectedCase.courtName }}</p>
              <p><span class="text-muted-foreground">原告：</span>{{ selectedCase.plaintiff }}</p>
              <p><span class="text-muted-foreground">被告：</span>{{ selectedCase.defendant }}</p>
              <p><span class="text-muted-foreground">主办律师：</span>{{ selectedCase.leadAttorneyName }}</p>
            </div>
          </div>

          <!-- Selected Template Info -->
          <div v-if="selectedTemplate" class="space-y-2 p-4 bg-muted rounded-lg">
            <h4 class="font-semibold text-sm">选中的模板</h4>
            <div class="space-y-1 text-sm">
              <p><span class="text-muted-foreground">模板名称：</span>{{ selectedTemplate.name }}</p>
              <p><span class="text-muted-foreground">模板类型：</span>{{ selectedTemplate.type }}</p>
              <p><span class="text-muted-foreground">描述：</span>{{ selectedTemplate.description }}</p>
              <p v-if="selectedTemplate.creator" class="text-xs text-muted-foreground">
                创建者：{{ selectedTemplate.creator }}
              </p>
            </div>
          </div>
        </div>

        <!-- Warning if incomplete -->
        <Alert v-if="!selectedCase || !selectedTemplate" variant="default">
          <SafeIcon name="AlertCircle" :size="16" class="mr-2" />
          <AlertDescription>
            请完成上述两个步骤后再生成文书
          </AlertDescription>
        </Alert>
      </CardContent>
    </Card>

    <!-- Action Buttons -->
    <div class="flex justify-end gap-3">
      <Button 
        variant="outline" 
        @click="handleBack"
      >
        <SafeIcon name="ArrowLeft" :size="16" class="mr-2" />
        返回
      </Button>
      <Button 
        :disabled="!canGenerate"
        @click="handleGenerate"
        class="gap-2"
      >
        <SafeIcon v-if="!isGenerating" name="Zap" :size="16" />
        <span v-if="isGenerating" class="inline-block animate-spin">
          <SafeIcon name="Loader2" :size="16" />
        </span>
        {{ isGenerating ? '生成中...' : '生成文书初稿' }}
      </Button>
    </div>
  </div>
</template>
