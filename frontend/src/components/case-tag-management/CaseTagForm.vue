
<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { FormField, FormItem, FormLabel, FormControl, FormMessage } from '@/components/ui/form'
import SafeIcon from '@/components/common/SafeIcon.vue'
import type { CaseTagModel } from '@/data/case'

interface Props {
  initialTag?: CaseTagModel
}

const props = defineProps<Props>()

const emit = defineEmits<{
  submit: [tag: CaseTagModel | Omit<CaseTagModel, 'id'>]
  close: []
}>()

// Color options
const colorOptions = [
  { value: 'bg-red-500', label: '红色' },
  { value: 'bg-yellow-500', label: '黄色' },
  { value: 'bg-purple-500', label: '紫色' },
  { value: 'bg-blue-500', label: '蓝色' },
  { value: 'bg-gray-500', label: '灰色' },
  { value: 'bg-green-500', label: '绿色' },
  { value: 'bg-pink-500', label: '粉色' },
  { value: 'bg-indigo-500', label: '靛蓝' },
]

// Form state
const tagName = ref('')
const selectedColor = ref('bg-blue-500')
const errors = ref<Record<string, string>>({})

// Initialize with existing tag if provided
watch(
  () => props.initialTag,
  (tag) => {
    if (tag) {
      tagName.value = tag.name
      selectedColor.value = tag.color
    }
  },
  { immediate: true }
)

// Validation
const validateForm = () => {
  errors.value = {}
  
  if (!tagName.value.trim()) {
    errors.value.tagName = '标签名称不能为空'
  }
  
  if (tagName.value.length > 20) {
    errors.value.tagName = '标签名称不能超过20个字符'
  }
  
  return Object.keys(errors.value).length === 0
}

// Submit
const handleSubmit = () => {
  if (!validateForm()) return
  
  const tagData = props.initialTag
    ? {
        id: props.initialTag.id,
        name: tagName.value.trim(),
        color: selectedColor.value,
      }
    : {
        name: tagName.value.trim(),
        color: selectedColor.value,
      }
  
  emit('submit', tagData)
}

// Cancel
const handleCancel = () => {
  emit('close')
}
</script>

<template>
  <div class="space-y-4">
    <!-- Tag Name Input -->
    <FormItem>
      <FormLabel>标签名称</FormLabel>
      <FormControl>
        <Input
          v-model="tagName"
          placeholder="例如：紧急、重大、合同类"
          maxlength="20"
          @keyup.enter="handleSubmit"
        />
      </FormControl>
      <FormMessage v-if="errors.tagName" class="text-destructive text-sm">
        {{ errors.tagName }}
      </FormMessage>
      <p class="text-xs text-muted-foreground mt-1">
        {{ tagName.length }}/20
      </p>
    </FormItem>

    <!-- Color Selection -->
    <FormItem>
      <FormLabel>选择颜色</FormLabel>
      <div class="grid grid-cols-4 gap-2">
        <button
          v-for="color in colorOptions"
          :key="color.value"
          :class="[
            'h-10 rounded-lg border-2 transition-all',
            selectedColor === color.value
              ? 'border-foreground ring-2 ring-offset-2 ring-primary'
              : 'border-transparent hover:border-border',
            color.value,
          ]"
          :title="color.label"
          @click="selectedColor = color.value"
        />
      </div>
    </FormItem>

    <!-- Preview -->
    <FormItem>
      <FormLabel>预览</FormLabel>
      <div class="flex items-center gap-2 p-3 bg-muted rounded-lg">
        <div :class="['h-6 w-6 rounded', selectedColor]" />
        <span class="text-sm font-medium">{{ tagName || '标签名称' }}</span>
      </div>
    </FormItem>

    <!-- Actions -->
    <div class="flex gap-2 justify-end pt-4">
      <Button variant="outline" @click="handleCancel">
        取消
      </Button>
      <Button @click="handleSubmit">
        <SafeIcon name="Check" :size="16" class="mr-2" />
        {{ initialTag ? '保存修改' : '创建标签' }}
      </Button>
    </div>
  </div>
</template>
