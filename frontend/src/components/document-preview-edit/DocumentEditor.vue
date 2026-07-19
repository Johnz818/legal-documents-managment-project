
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Textarea } from '@/components/ui/textarea'
import { Button } from '@/components/ui/button'
import SafeIcon from '@/components/common/SafeIcon.vue'

interface Props {
  modelValue: string
}

interface Emits {
  (e: 'update:modelValue', value: string): void
}

defineProps<Props>()
defineEmits<Emits>()

const editorContent = ref('')
const isFullscreen = ref(false)

onMounted(() => {
  if (typeof window !== 'undefined') {
    // 编辑器初始化逻辑
  }
})

const handleInput = (event: Event) => {
  const target = event.target as HTMLTextAreaElement
  editorContent.value = target.value
}

const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
}

const insertVariable = (variable: string) => {
  const textarea = document.querySelector('textarea') as HTMLTextAreaElement
  if (textarea) {
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const text = editorContent.value
    editorContent.value = text.substring(0, start) + variable + text.substring(end)
    textarea.focus()
    textarea.setSelectionRange(start + variable.length, start + variable.length)
  }
}

const variables = [
  { label: '案号', value: '{{案号}}' },
  { label: '法院', value: '{{法院}}' },
  { label: '原告', value: '{{原告}}' },
  { label: '被告', value: '{{被告}}' },
  { label: '案由', value: '{{案由}}' },
  { label: '主办律师', value: '{{主办律师}}' },
  { label: '开庭时间', value: '{{开庭时间}}' },
  { label: '当前日期', value: '{{当前日期}}' },
]
</script>

<template>
  <div :class="['flex flex-col h-full', { 'fixed inset-0 z-50 bg-background': isFullscreen }]">
    <!-- 编辑器工具栏 -->
    <div class="border-b bg-background px-4 py-3 space-y-3">
      <!-- 主工具栏 -->
      <div class="flex items-center justify-between gap-2">
        <div class="flex items-center gap-2">
          <span class="text-sm font-medium">快速插入变量：</span>
          <div class="flex flex-wrap gap-2">
            <Button
              v-for="variable in variables"
              :key="variable.value"
              variant="outline"
              size="sm"
              @click="insertVariable(variable.value)"
              class="text-xs"
            >
              {{ variable.label }}
            </Button>
          </div>
        </div>
        <Button
          variant="ghost"
          size="icon"
          @click="toggleFullscreen"
          :title="isFullscreen ? '退出全屏' : '全屏编辑'"
        >
          <SafeIcon :name="isFullscreen ? 'Minimize' : 'Maximize'" :size="18" />
        </Button>
      </div>

      <!-- 编辑提示 -->
      <div class="text-xs text-muted-foreground bg-muted p-2 rounded">
        💡 提示：点击上方变量按钮快速插入，或手动输入 {{变量名}} 格式的占位符
      </div>
    </div>

    <!-- 编辑区域 -->
    <div class="flex-1 overflow-hidden p-4">
      <Textarea
        :value="modelValue"
        @input="handleInput"
        @update:model-value="$emit('update:modelValue', $event)"
        placeholder="在此输入或编辑文书内容..."
        class="h-full resize-none font-mono text-sm"
      />
    </div>

    <!-- 字数统计 -->
    <div class="border-t bg-muted px-4 py-2 text-xs text-muted-foreground">
      字数统计：{{ modelValue.length }} 字
    </div>
  </div>
</template>
