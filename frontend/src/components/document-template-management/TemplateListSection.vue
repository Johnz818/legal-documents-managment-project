
<script setup lang="ts">
import type { DocumentTemplateModel } from '@/data/document'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import TemplateCard from './TemplateCard.vue'

interface Props {
  title: string
  templates: DocumentTemplateModel[]
  isPreset: boolean
}

defineProps<Props>()
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center gap-2">
      <h2 class="text-lg font-semibold">{{ title }}</h2>
      <span class="text-sm text-muted-foreground">({{ templates.length }})</span>
    </div>
    
    <div class="grid gap-4 grid-cols-1 md:grid-cols-2 lg:grid-cols-3">
      <TemplateCard
        v-for="template in templates"
        :key="template.templateId"
        :template="template"
        :is-preset="isPreset"
      />
    </div>
  </div>
</template>
