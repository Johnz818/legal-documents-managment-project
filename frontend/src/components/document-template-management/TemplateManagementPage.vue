
<script setup lang="ts">
import { ref } from 'vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import SafeIcon from '@/components/common/SafeIcon.vue'
import TemplateListSection from './TemplateListSection.vue'
import CreateTemplateDialog from './CreateTemplateDialog.vue'
import { MOCK_DOCUMENT_TEMPLATES, TemplateType } from '@/data/document'

const searchQuery = ref('')
const showCreateDialog = ref(false)

const presetTemplates = MOCK_DOCUMENT_TEMPLATES.filter(t => t.type === TemplateType.Preset)
const customTemplates = MOCK_DOCUMENT_TEMPLATES.filter(t => t.type === TemplateType.Custom)

const filteredPresetTemplates = presetTemplates.filter(t =>
  t.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
  t.description.toLowerCase().includes(searchQuery.value.toLowerCase())
)

const filteredCustomTemplates = customTemplates.filter(t =>
  t.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
  t.description.toLowerCase().includes(searchQuery.value.toLowerCase())
)
</script>

<template>
  <div class="flex flex-col gap-6 p-6">
    <!-- 搜索和操作栏 -->
    <div class="flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between">
      <div class="flex-1 w-full sm:w-auto">
        <Input
          v-model="searchQuery"
          placeholder="搜索模板名称或描述..."
          class="w-full"
        />
      </div>
      <Button @click="showCreateDialog = true" class="gap-2">
        <SafeIcon name="Plus" :size="18" />
        创建自定义模板
      </Button>
    </div>

    <!-- 模板分类标签页 -->
    <Tabs defaultValue="all" class="w-full">
      <TabsList class="grid w-full grid-cols-3">
        <TabsTrigger value="all">
          全部模板 ({{ presetTemplates.length + customTemplates.length }})
        </TabsTrigger>
        <TabsTrigger value="preset">
          系统预设 ({{ presetTemplates.length }})
        </TabsTrigger>
        <TabsTrigger value="custom">
          自定义 ({{ customTemplates.length }})
        </TabsTrigger>
      </TabsList>

      <!-- 全部模板 -->
      <TabsContent value="all" class="space-y-6">
        <TemplateListSection
          v-if="filteredPresetTemplates.length > 0"
          title="系统预设模板"
          :templates="filteredPresetTemplates"
          :is-preset="true"
        />
        <TemplateListSection
          v-if="filteredCustomTemplates.length > 0"
          title="用户自定义模板"
          :templates="filteredCustomTemplates"
          :is-preset="false"
        />
        <div v-if="filteredPresetTemplates.length === 0 && filteredCustomTemplates.length === 0" class="text-center py-12">
          <SafeIcon name="Search" :size="48" class="mx-auto text-muted-foreground mb-4" />
          <p class="text-muted-foreground">未找到匹配的模板</p>
        </div>
      </TabsContent>

      <!-- 系统预设模板 -->
      <TabsContent value="preset" class="space-y-6">
        <TemplateListSection
          v-if="filteredPresetTemplates.length > 0"
          title="系统预设模板"
          :templates="filteredPresetTemplates"
          :is-preset="true"
        />
        <div v-else class="text-center py-12">
          <SafeIcon name="FileText" :size="48" class="mx-auto text-muted-foreground mb-4" />
          <p class="text-muted-foreground">暂无预设模板</p>
        </div>
      </TabsContent>

      <!-- 自定义模板 -->
      <TabsContent value="custom" class="space-y-6">
        <TemplateListSection
          v-if="filteredCustomTemplates.length > 0"
          title="用户自定义模板"
          :templates="filteredCustomTemplates"
          :is-preset="false"
        />
        <div v-else class="text-center py-12">
          <SafeIcon name="Plus" :size="48" class="mx-auto text-muted-foreground mb-4" />
          <p class="text-muted-foreground mb-4">您还没有创建自定义模板</p>
          <Button @click="showCreateDialog = true" variant="outline" class="gap-2">
            <SafeIcon name="Plus" :size="18" />
            创建第一个模板
          </Button>
        </div>
      </TabsContent>
    </Tabs>

    <!-- 创建模板对话框 -->
    <CreateTemplateDialog
      :open="showCreateDialog"
      @close="showCreateDialog = false"
    />
  </div>
</template>
