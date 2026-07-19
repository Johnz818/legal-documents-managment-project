
<script setup lang="ts">
import type { DocumentTemplateModel } from '@/data/document'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'

interface Props {
  template: DocumentTemplateModel
}

defineProps<Props>()

const emit = defineEmits<{
  preview: [template: DocumentTemplateModel]
}>()

const handlePreview = () => {
  emit('preview', props.template)
}
</script>

<template>
  <Card class="flex flex-col hover:shadow-card transition-shadow">
    <CardHeader>
      <div class="flex items-start justify-between gap-2">
        <div class="flex-1">
          <CardTitle class="text-base">{{ template.name }}</CardTitle>
          <CardDescription class="mt-1">
            {{ template.description }}
          </CardDescription>
        </div>
        <Badge variant="secondary" class="shrink-0">
          {{ template.type }}
        </Badge>
      </div>
    </CardHeader>

    <CardContent class="flex-1 flex flex-col gap-4">
      <!-- Template Info -->
      <div class="space-y-2 text-sm">
        <div class="flex items-center gap-2 text-muted-foreground">
          <SafeIcon name="Calendar" :size="16" />
          <span>最后修改：{{ template.lastModified }}</span>
        </div>
        <div class="flex items-center gap-2 text-muted-foreground">
          <SafeIcon name="FileText" :size="16" />
          <span>模板ID：{{ template.templateId }}</span>
        </div>
      </div>

      <!-- Content Preview (truncated) -->
      <div class="bg-muted p-3 rounded-md max-h-24 overflow-hidden">
        <p class="text-xs text-muted-foreground whitespace-pre-wrap line-clamp-4">
          {{ template.contentTemplate }}
        </p>
      </div>

      <!-- Action Button -->
      <Button 
        variant="outline" 
        size="sm" 
        class="w-full mt-auto"
        @click="handlePreview"
      >
        <SafeIcon name="Eye" :size="16" class="mr-2" />
        查看详情
      </Button>
    </CardContent>
  </Card>
</template>
