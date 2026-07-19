
<script setup lang="ts">
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import SafeIcon from '@/components/common/SafeIcon.vue'

interface Template {
  id: string
  name: string
  description: string
  category: string
  usageCount: number
}

interface Props {
  templates: Template[]
}

defineProps<Props>()

const getCategoryColor = (category: string) => {
  const colors: Record<string, string> = {
    '民事诉讼': 'bg-blue-100 text-blue-800',
    '证据管理': 'bg-green-100 text-green-800',
    '刑事诉讼': 'bg-red-100 text-red-800',
    '行政诉讼': 'bg-purple-100 text-purple-800',
  }
  return colors[category] || 'bg-gray-100 text-gray-800'
}
</script>

<template>
  <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
    <Card
      v-for="template in templates"
      :key="template.id"
      class="hover:shadow-md transition-shadow"
    >
      <CardHeader class="pb-3">
        <div class="flex items-start justify-between gap-2">
          <div class="flex-1">
            <CardTitle class="text-base">{{ template.name }}</CardTitle>
            <CardDescription class="text-xs mt-1">{{ template.description }}</CardDescription>
          </div>
          <SafeIcon name="FileText" :size="20" class="text-primary flex-shrink-0" />
        </div>
      </CardHeader>
      <CardContent class="space-y-3">
        <!-- Category Badge -->
        <div>
          <Badge :class="getCategoryColor(template.category)" variant="outline">
            {{ template.category }}
          </Badge>
        </div>

        <!-- Usage Stats -->
        <div class="flex items-center gap-2 text-xs text-muted-foreground">
          <SafeIcon name="TrendingUp" :size="14" />
          <span>使用次数：{{ template.usageCount }}</span>
        </div>

        <!-- Action Button -->
        <Button
          as="a"
          href="./document-generate-selection.html"
          variant="outline"
          size="sm"
          class="w-full"
        >
          使用此模板
        </Button>
      </CardContent>
    </Card>
  </div>
</template>
