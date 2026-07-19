
<script setup lang="ts">
import { ref, computed } from 'vue'
import { MOCK_DOCUMENT_TEMPLATES, TemplateType, type DocumentTemplateModel } from '@/data/document'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import SafeIcon from '@/components/common/SafeIcon.vue'
import PresetTemplateCard from './PresetTemplateCard.vue'
import TemplatePreviewModal from './TemplatePreviewModal.vue'

// Filter only preset templates
const presetTemplates = MOCK_DOCUMENT_TEMPLATES.filter(t => t.type === TemplateType.Preset)

const searchQuery = ref('')
const selectedTemplate = ref<DocumentTemplateModel | null>(null)
const showPreviewModal = ref(false)

const filteredTemplates = computed(() => {
   if (!searchQuery.value) return presetTemplates
   
   const query = searchQuery.value.toLowerCase()
   return presetTemplates.filter(t => 
     t.name.toLowerCase().includes(query) || 
     t.description.toLowerCase().includes(query)
   )
 })

const openPreview = (template: DocumentTemplateModel) => {
   selectedTemplate.value = template
   showPreviewModal.value = true
 }

const closePreview = () => {
   showPreviewModal.value = false
   selectedTemplate.value = null
 }
</script>

<template>
  <div class="flex-1 flex flex-col">
    <!-- Search Bar -->
    <div class="border-b px-6 py-4">
      <div class="flex items-center gap-2">
        <SafeIcon name="Search" :size="18" class="text-muted-foreground" />
        <Input
          v-model="searchQuery"
          type="text"
          placeholder="搜索模板名称或描述..."
          class="flex-1"
        />
      </div>
    </div>

    <!-- Templates Grid -->
    <div class="flex-1 overflow-auto p-6">
      <div v-if="filteredTemplates.length === 0" class="flex flex-col items-center justify-center py-12">
        <SafeIcon name="FileText" :size="48" class="text-muted-foreground mb-4" />
        <p class="text-lg font-medium text-foreground mb-2">未找到匹配的模板</p>
        <p class="text-sm text-muted-foreground">请尝试调整搜索条件</p>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <PresetTemplateCard
          v-for="template in filteredTemplates"
          :key="template.templateId"
          :template="template"
          @preview="openPreview"
        />
      </div>
    </div>

    <!-- Footer with Back Button -->
    <div class="border-t px-6 py-4 flex justify-between items-center">
      <p class="text-sm text-muted-foreground">
        共 {{ filteredTemplates.length }} 个模板
      </p>
      <Button variant="outline" as="a" href="./document-template-management.html">
        <SafeIcon name="ArrowLeft" :size="16" class="mr-2" />
        返回模板管理
      </Button>
    </div>

    <!-- Preview Modal -->
    <TemplatePreviewModal
      v-if="showPreviewModal && selectedTemplate"
      :template="selectedTemplate"
      @close="closePreview"
    />
  </div>
</template>
