
<script setup lang="ts">
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import SafeIcon from '@/components/common/SafeIcon.vue'
import type { TemplateInfoModel } from '@/data/case'

interface Props {
  templateInfo: TemplateInfoModel
}

defineProps<Props>()

const emit = defineEmits<{
  download: []
}>()
</script>

<template>
  <Card>
    <CardHeader>
      <CardTitle class="flex items-center gap-2">
        <SafeIcon name="FileSpreadsheet" :size="24" class="text-primary" />
        {{ templateInfo.name }}
      </CardTitle>
      <CardDescription>{{ templateInfo.description }}</CardDescription>
    </CardHeader>
    <CardContent class="space-y-4">
      <!-- Template Preview Image -->
      <div class="rounded-lg border overflow-hidden bg-muted">
        <img 
          :src="templateInfo.exampleImageUrl" 
          alt="模板预览"
          class="w-full h-auto object-cover"
        />
      </div>

      <!-- Fields Info -->
      <div class="space-y-2">
        <p class="text-sm font-medium">包含字段：</p>
        <p class="text-sm text-muted-foreground">{{ templateInfo.fields }}</p>
      </div>

      <!-- Download Button -->
      <Button @click="() => emit('download')" class="w-full" size="lg">
        <SafeIcon name="Download" :size="18" class="mr-2" />
        下载Excel模板
      </Button>
    </CardContent>
  </Card>
</template>
