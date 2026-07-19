
<script setup lang="ts">
import { ref, computed } from 'vue'
import type { DocumentTemplateModel } from '@/data/document'
import { MOCK_TEMPLATE_VARIABLES } from '@/data/document'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'

interface Props {
  template: DocumentTemplateModel
}

const props = defineProps<Props>()

const emit = defineEmits<{
  close: []
}>()

const activeTab = ref('content')

// Extract variables used in this template
const usedVariables = computed(() => {
  const regex = /\{\{([^}]+)\}\}/g
  const matches = [...props.template.contentTemplate.matchAll(regex)]
  const variableNames = matches.map(m => `{{${m[1]}}}`)
  
  return MOCK_TEMPLATE_VARIABLES.filter(v => 
    variableNames.includes(v.variableName)
  )
})

const handleClose = () => {
  emit('close')
}
</script>

<template>
  <Dialog :open="true" @update:open="handleClose">
    <DialogContent class="max-w-3xl max-h-[80vh] overflow-y-auto">
      <DialogHeader>
        <DialogTitle class="flex items-center gap-2">
          <SafeIcon name="FileText" :size="20" />
          {{ template.name }}
        </DialogTitle>
        <DialogDescription>
          {{ template.description }}
        </DialogDescription>
      </DialogHeader>

      <Tabs v-model="activeTab" class="w-full">
        <TabsList class="grid w-full grid-cols-2">
          <TabsTrigger value="content">模板内容</TabsTrigger>
          <TabsTrigger value="variables">可用变量</TabsTrigger>
        </TabsList>

        <!-- Content Tab -->
        <TabsContent value="content" class="space-y-4">
          <div class="bg-muted p-4 rounded-lg border">
            <p class="text-sm whitespace-pre-wrap font-mono text-foreground">
              {{ template.contentTemplate }}
            </p>
          </div>
          <div class="flex items-center gap-2 text-xs text-muted-foreground">
            <SafeIcon name="Info" :size="14" />
            <span>此模板为系统预设，不可编辑。如需自定义，请创建新的用户自定义模板。</span>
          </div>
        </TabsContent>

        <!-- Variables Tab -->
        <TabsContent value="variables" class="space-y-4">
          <div v-if="usedVariables.length > 0" class="space-y-3">
            <p class="text-sm text-muted-foreground">
              此模板使用了以下 {{ usedVariables.length }} 个变量：
            </p>
            <div class="space-y-2">
              <div
                v-for="variable in usedVariables"
                :key="variable.variableName"
                class="flex items-start gap-3 p-3 bg-muted rounded-lg"
              >
                <Badge variant="outline" class="shrink-0 mt-0.5">
                  {{ variable.variableName }}
                </Badge>
                <div class="flex-1">
                  <p class="text-sm font-medium text-foreground">
                    {{ variable.description }}
                  </p>
                  <p class="text-xs text-muted-foreground mt-1">
                    来源字段：{{ variable.sourceField }}
                  </p>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="flex items-center gap-2 text-sm text-muted-foreground">
            <SafeIcon name="Info" :size="16" />
            <span>此模板不使用任何动态变量</span>
          </div>
        </TabsContent>
      </Tabs>

      <DialogFooter class="flex gap-2 justify-end">
        <Button variant="outline" @click="handleClose">
          关闭
        </Button>
        <Button as="a" href="./document-generate-selection.html">
          <SafeIcon name="FileText" :size="16" class="mr-2" />
          使用此模板生成文书
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
