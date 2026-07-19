
<script setup lang="ts">
import { ref } from 'vue'
import type { DocumentTemplateModel } from '@/data/document'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import SafeIcon from '@/components/common/SafeIcon.vue'
import { MOCK_TEMPLATE_VARIABLES } from '@/data/document'

interface Props {
  open: boolean
  template: DocumentTemplateModel
}

interface Emits {
  (e: 'close'): void
}

const props = defineProps<Props>()
defineEmits<Emits>()

const templateName = ref(props.template.name)
const templateDescription = ref(props.template.description)
const templateContent = ref(props.template.contentTemplate)

const handleSave = () => {
  // 实际应用中应调用保存API
  console.log('保存模板:', {
    templateId: props.template.templateId,
    name: templateName.value,
    description: templateDescription.value,
    content: templateContent.value,
  })
  // 关闭对话框
  // emit('close')
}

const insertVariable = (variableName: string) => {
  const textarea = document.querySelector('textarea[data-template-content]') as HTMLTextAreaElement
  if (textarea) {
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const before = templateContent.value.substring(0, start)
    const after = templateContent.value.substring(end)
    templateContent.value = before + variableName + after
    // 重新设置光标位置
    setTimeout(() => {
      textarea.selectionStart = textarea.selectionEnd = start + variableName.length
      textarea.focus()
    }, 0)
  }
}
</script>

<template>
  <Dialog :open="open" @update:open="$emit('close')">
    <DialogContent class="max-w-4xl max-h-[90vh] flex flex-col">
      <DialogHeader>
        <DialogTitle>编辑模板: {{ template.name }}</DialogTitle>
        <DialogDescription>
          修改模板内容，支持使用占位符变量
        </DialogDescription>
      </DialogHeader>

      <Tabs defaultValue="edit" class="flex-1 flex flex-col">
        <TabsList class="grid w-full grid-cols-2">
          <TabsTrigger value="edit">编辑</TabsTrigger>
          <TabsTrigger value="variables">可用变量</TabsTrigger>
        </TabsList>

        <!-- 编辑标签页 -->
        <TabsContent value="edit" class="flex-1 flex flex-col gap-4 overflow-hidden">
          <div class="space-y-2">
            <Label htmlFor="template-name">模板名称</Label>
            <Input
              id="template-name"
              v-model="templateName"
              placeholder="输入模板名称"
            />
          </div>

          <div class="space-y-2">
            <Label htmlFor="template-description">模板描述</Label>
            <Input
              id="template-description"
              v-model="templateDescription"
              placeholder="输入模板描述"
            />
          </div>

          <div class="space-y-2 flex-1 flex flex-col">
            <Label htmlFor="template-content">模板内容</Label>
            <Textarea
              id="template-content"
              data-template-content
              v-model="templateContent"
              placeholder="输入模板内容，使用 {{变量名}} 插入占位符"
              class="flex-1 font-mono text-sm resize-none"
            />
          </div>
        </TabsContent>

        <!-- 可用变量标签页 -->
        <TabsContent value="variables" class="flex-1 flex flex-col overflow-hidden">
          <ScrollArea class="flex-1 border rounded-md p-4">
            <div class="space-y-3">
              <div
                v-for="variable in MOCK_TEMPLATE_VARIABLES"
                :key="variable.variableName"
                class="flex items-start justify-between gap-3 p-3 border rounded-md hover:bg-muted/50 transition-colors"
              >
                <div class="flex-1 min-w-0">
                  <div class="font-mono text-sm font-semibold text-primary">
                    {{ variable.variableName }}
                  </div>
                  <div class="text-sm text-muted-foreground">
                    {{ variable.description }}
                  </div>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  @click="insertVariable(variable.variableName)"
                  class="shrink-0"
                >
                  <SafeIcon name="Plus" :size="16" class="mr-1" />
                  插入
                </Button>
              </div>
            </div>
          </ScrollArea>
        </TabsContent>
      </Tabs>

      <div class="flex gap-2 justify-end pt-4 border-t">
        <Button variant="outline" @click="$emit('close')">
          取消
        </Button>
        <Button @click="handleSave" class="gap-2">
          <SafeIcon name="Save" :size="18" />
          保存模板
        </Button>
      </div>
    </DialogContent>
  </Dialog>
</template>
