<script setup lang="ts">
import type { DocumentTemplateSummary } from '@/types/documentGeneration'
import TemplateCard from './TemplateCard.vue'

defineProps<{ title: string; templates: DocumentTemplateSummary[] }>()
const emit = defineEmits<{
  view: [template: DocumentTemplateSummary]
  publishVersion: [template: DocumentTemplateSummary]
}>()
</script>

<template>
  <section class="space-y-4">
    <div class="flex items-center gap-2">
      <h2 class="text-lg font-semibold">{{ title }}</h2>
      <span class="text-sm text-muted-foreground">({{ templates.length }})</span>
    </div>
    <div class="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
      <TemplateCard
        v-for="template in templates"
        :key="template.id"
        :template="template"
        @view="emit('view', template)"
        @publish-version="emit('publishVersion', template)"
      />
    </div>
  </section>
</template>
