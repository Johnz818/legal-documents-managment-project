
<script setup lang="ts">
import { ref } from 'vue'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'
import type { TemplateVariableModel } from '@/data/document'

interface Props {
  variables: TemplateVariableModel[]
}

const emit = defineEmits<{
  insert: [variableName: string]
}>()

const searchQuery = ref('')

const filteredVariables = computed(() => {
  if (!searchQuery.value.trim()) {
    return props.variables
  }
  const query = searchQuery.value.toLowerCase()
  return props.variables.filter(v =>
    v.variableName.toLowerCase().includes(query) ||
    v.description.toLowerCase().includes(query)
  )
})

import { computed } from 'vue'
</script>

<template>
  <Card class="sticky top-0">
    <CardHeader>
      <CardTitle class="text-base">可用占位符</CardTitle>
      <CardDescription>点击插入到模板内容</CardDescription>
    </CardHeader>
    <CardContent class="space-y-4">
      <!-- 搜索框 -->
      <div class="space-y-2">
        <Input
          v-model="searchQuery"
          placeholder="搜索占位符..."
          class="w-full"
        />
      </div>

      <!-- 变量列表 -->
      <ScrollArea class="h-96 w-full rounded-md border p-3">
        <div class="space-y-2">
          <div
            v-for="variable in filteredVariables"
            :key="variable.variableName"
            class="group flex items-start gap-2 p-2 rounded-md hover:bg-muted transition-colors"
          >
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <Badge variant="secondary" class="font-mono text-xs">
                  {{ variable.variableName }}
                </Badge>
              </div>
              <p class="text-xs text-muted-foreground line-clamp-2">
                {{ variable.description }}
              </p>
            </div>
            <Button
              size="sm"
              variant="ghost"
              class="shrink-0 opacity-0 group-hover:opacity-100 transition-opacity"
              @click="emit('insert', variable.variableName)"
              title="点击插入"
            >
              <SafeIcon name="Plus" :size="16" />
            </Button>
          </div>

          <div v-if="filteredVariables.length === 0" class="text-center py-8">
            <p class="text-sm text-muted-foreground">未找到匹配的占位符</p>
          </div>
        </div>
      </ScrollArea>

      <!-- 帮助提示 -->
      <div class="bg-blue-50 dark:bg-blue-950 rounded-md p-3 space-y-2">
        <div class="flex items-start gap-2">
          <SafeIcon name="Info" :size="16" class="text-blue-600 dark:text-blue-400 mt-0.5 shrink-0" />
          <div class="text-xs text-blue-700 dark:text-blue-300">
            <p class="font-semibold mb-1">使用提示：</p>
            <ul class="list-disc list-inside space-y-1">
              <li>点击"+"按钮快速插入占位符</li>
              <li>或手动输入 {{变量名}} 格式</li>
              <li>生成文书时系统会自动替换</li>
            </ul>
          </div>
        </div>
      </div>
    </CardContent>
  </Card>
</template>
