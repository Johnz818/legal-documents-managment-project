
<script setup lang="ts">
import { computed } from 'vue'
import { Button } from '@/components/ui/button'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'
import type { DocumentTemplateModel, TemplateType } from '@/data/document'

interface Props {
  templates: DocumentTemplateModel[]
  selectedTemplateId: string | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  select: [templateId: string]
}>()

const presetTemplates = computed(() => 
  props.templates.filter(t => t.type === '系统预设')
)

const customTemplates = computed(() => 
  props.templates.filter(t => t.type === '用户自定义')
)

const handleSelect = (templateId: string) => {
  emit('select', templateId)
}

const getTypeColor = (type: string) => {
  return type === '系统预设' ? 'bg-blue-100 text-blue-800' : 'bg-purple-100 text-purple-800'
}
</script>

<template>
  <div class="space-y-4">
    <Tabs defaultValue="preset" class="w-full">
      <TabsList class="grid w-full grid-cols-2">
        <TabsTrigger value="preset">
          系统预设 ({{ presetTemplates.length }})
        </TabsTrigger>
        <TabsTrigger value="custom">
          用户自定义 ({{ customTemplates.length }})
        </TabsTrigger>
      </TabsList>

      <!-- Preset Templates -->
      <TabsContent value="preset" class="space-y-3 mt-4">
        <div v-if="presetTemplates.length > 0" class="space-y-3">
          <Card 
            v-for="template in presetTemplates"
            :key="template.templateId"
            class="cursor-pointer transition-all hover:shadow-md"
            :class="selectedTemplateId === template.templateId ? 'ring-2 ring-primary' : ''"
            @click="handleSelect(template.templateId)"
          >
            <CardHeader class="pb-3">
              <div class="flex items-start justify-between gap-2">
                <div class="flex-1">
                  <CardTitle class="text-base">{{ template.name }}</CardTitle>
                  <CardDescription class="text-xs mt-1">
                    {{ template.description }}
                  </CardDescription>
                </div>
                <Badge :class="getTypeColor(template.type)" class="shrink-0">
                  {{ template.type }}
                </Badge>
              </div>
            </CardHeader>
            <CardContent>
              <div class="flex items-center justify-between">
                <p class="text-xs text-muted-foreground">
                  最后修改：{{ template.lastModified }}
                </p>
                <div 
                  class="flex h-5 w-5 items-center justify-center rounded-full border"
                  :class="selectedTemplateId === template.templateId ? 'bg-primary border-primary' : 'border-muted-foreground'"
                >
                  <SafeIcon 
                    v-if="selectedTemplateId === template.templateId"
                    name="Check" 
                    :size="14" 
                    color="white"
                  />
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
        <div v-else class="flex flex-col items-center justify-center py-8 text-center">
          <SafeIcon name="FileText" :size="32" class="text-muted-foreground mb-2" />
          <p class="text-sm text-muted-foreground">暂无系统预设模板</p>
        </div>
      </TabsContent>

      <!-- Custom Templates -->
      <TabsContent value="custom" class="space-y-3 mt-4">
        <div v-if="customTemplates.length > 0" class="space-y-3">
          <Card 
            v-for="template in customTemplates"
            :key="template.templateId"
            class="cursor-pointer transition-all hover:shadow-md"
            :class="selectedTemplateId === template.templateId ? 'ring-2 ring-primary' : ''"
            @click="handleSelect(template.templateId)"
          >
            <CardHeader class="pb-3">
              <div class="flex items-start justify-between gap-2">
                <div class="flex-1">
                  <CardTitle class="text-base">{{ template.name }}</CardTitle>
                  <CardDescription class="text-xs mt-1">
                    {{ template.description }}
                  </CardDescription>
                </div>
                <Badge :class="getTypeColor(template.type)" class="shrink-0">
                  {{ template.type }}
                </Badge>
              </div>
            </CardHeader>
            <CardContent>
              <div class="flex items-center justify-between">
                <p class="text-xs text-muted-foreground">
                  最后修改：{{ template.lastModified }}
                </p>
                <div 
                  class="flex h-5 w-5 items-center justify-center rounded-full border"
                  :class="selectedTemplateId === template.templateId ? 'bg-primary border-primary' : 'border-muted-foreground'"
                >
                  <SafeIcon 
                    v-if="selectedTemplateId === template.templateId"
                    name="Check" 
                    :size="14" 
                    color="white"
                  />
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
        <div v-else class="flex flex-col items-center justify-center py-8 text-center">
          <SafeIcon name="FileText" :size="32" class="text-muted-foreground mb-2" />
          <p class="text-sm text-muted-foreground">暂无自定义模板</p>
          <Button 
            variant="outline" 
            size="sm" 
            as="a"
            href="./document-template-custom-create-edit.html"
            class="mt-3"
          >
            <SafeIcon name="Plus" :size="14" class="mr-1" />
            创建模板
          </Button>
        </div>
      </TabsContent>
    </Tabs>
  </div>
</template>
