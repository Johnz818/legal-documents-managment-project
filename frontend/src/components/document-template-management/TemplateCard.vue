<script setup lang="ts">
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import SafeIcon from '@/components/common/SafeIcon.vue'
import type { DocumentTemplateSummary } from '@/types/documentGeneration'

defineProps<{ template: DocumentTemplateSummary }>()
const emit = defineEmits<{ view: []; publishVersion: [] }>()
</script>

<template>
  <Card class="flex flex-col transition-shadow hover:shadow-card">
    <CardHeader class="pb-3">
      <div class="flex items-start justify-between gap-2">
        <div class="min-w-0 flex-1">
          <CardTitle class="truncate text-base">{{ template.name }}</CardTitle>
          <CardDescription class="mt-1 line-clamp-2">{{ template.description || '暂无描述' }}</CardDescription>
        </div>
        <Badge variant="outline" class="shrink-0">{{ template.templateType === 'CUSTOM' ? '自定义' : '系统预设' }}</Badge>
      </div>
    </CardHeader>
    <CardContent class="flex-1 text-sm text-muted-foreground">
      <p>模板编号：{{ template.id }}</p>
    </CardContent>
    <div class="flex gap-2 border-t p-3">
      <Button variant="outline" size="sm" class="flex-1" @click="emit('view')">
        <SafeIcon name="Eye" :size="16" class="mr-1" />查看版本
      </Button>
      <Button
        v-if="template.templateType === 'CUSTOM'"
        variant="outline"
        size="sm"
        class="flex-1"
        @click="emit('publishVersion')"
      >
        <SafeIcon name="Upload" :size="16" class="mr-1" />发布新版本
      </Button>
    </div>
  </Card>
</template>
