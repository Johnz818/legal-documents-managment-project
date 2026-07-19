
<script setup lang="ts">
import { ref } from 'vue'
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
}

interface Emits {
  (e: 'close'): void
}

defineProps<Props>()
defineEmits<Emits>()

const templateName = ref('')
const templateDescription = ref('')
const templateContent = ref('')

const handleCreate = () => {
  if (!templateName.value.trim()) {
    alert('请输入模板名称')
    return
  }
  if (!templateContent.value.trim()) {
    alert('请输入模板内容')
    return
  }

  // 实际应用中应调用创建API
  console.log('创建新模板:', {
    name: templateName.value,
    description: templateDescription.value,
    content: templateContent.value,
  })

  // 重置表单
  templateName.value = ''
  templateDescription.value = ''
  templateContent.value = ''

  // 关闭对话框
  // emit('close')
}

const insertVariable = (variableName: string) => {
  const textarea = document.querySelector('textarea[data-create-template-content]') as HTMLTextAreaElement
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
        <DialogTitle>创建自定义模板</DialogTitle>
        <DialogDescription>
          创建新的文书模板，支持使用占位符变量自动填充案件信息
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
            <Label htmlFor="new-template-name">模板名称 *</Label>
            <Input
              id="new-template-name"
              v-model="templateName"
              placeholder="例如：民事起诉状（自定义版）"
            />
          </div>

          <div class="space-y-2">
            <Label htmlFor="new-template-description">模板描述</Label>
            <Input
              id="new-template-description"
              v-model="templateDescription"
              placeholder="简要描述此模板的用途"
            />
          </div>

          <div class="space-y-2 flex-1 flex flex-col">
            <Label htmlFor="new-template-content">模板内容 *</Label>
            <Textarea
              id="new-template-content"
              data-create-template-content
              v-model="templateContent"
              placeholder="输入模板内容，使用 {{变量名}} 插入占位符，例如 {{原告}}、{{被告}} 等"
              class="flex-1 font-mono text-sm resize-none"
            />
            <p class="text-xs text-muted-foreground">
              提示：点击右侧"可用变量"标签页查看所有可用的占位符变量
            </p>
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
        <Button @click="handleCreate" class="gap-2">
          <SafeIcon name="Plus" :size="18" />
          创建模板
        </Button>
      </div>
    </DialogContent>
  </Dialog>
</template>
