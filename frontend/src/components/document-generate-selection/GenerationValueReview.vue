<script setup lang="ts">
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import type { PreparedGenerationField, StaleValueConflict } from '@/types/documentGeneration'

const props = defineProps<{
  fields: PreparedGenerationField[]
  values: Record<string, string>
  fieldErrors: Record<string, string>
  formError?: string
  disabled: boolean
  conflicts: StaleValueConflict[]
}>()

const emit = defineEmits<{
  updateValue: [fieldKey: string, value: string]
  resolveConflict: [fieldKey: string, resolution: 'USE_CURRENT' | 'KEEP_PREVIOUS']
}>()

const updateText = (fieldKey: string, value: string | number) => {
  emit('updateValue', fieldKey, String(value))
}

const updateSelect = (fieldKey: string, event: Event) => {
  emit('updateValue', fieldKey, (event.target as HTMLSelectElement).value)
}

const sourceLabel = (field: PreparedGenerationField) => {
  if (field.status === 'REQUIRES_USER_INPUT') return '需要手动输入'
  if (field.defaultSource === 'CASE_FIELD') return '案件信息建议值'
  if (field.defaultSource === 'SYSTEM_VALUE') return '系统建议值'
  return '手动输入'
}

const conflictFor = (fieldKey: string) => props.conflicts.find(
  conflict => conflict.fieldKey === fieldKey,
)
</script>

<template>
  <div class="space-y-5">
    <div v-if="formError" role="alert" class="rounded-md border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive">
      {{ formError }}
    </div>

    <div v-if="fields.length === 0" class="rounded-lg border border-dashed p-6 text-center text-sm text-muted-foreground">
      此模板没有占位字段，可以直接生成文书。
    </div>

    <div
      v-for="field in fields"
      :key="field.fieldKey"
      class="space-y-2 rounded-lg border p-4"
      :class="fieldErrors[field.fieldKey] || conflictFor(field.fieldKey) ? 'border-destructive bg-destructive/5' : ''"
    >
      <div class="flex flex-wrap items-center justify-between gap-2">
        <label :for="`generation-${field.fieldKey}`" class="text-sm font-medium">
          {{ field.displayName }}
          <span v-if="field.required" class="text-destructive">*</span>
        </label>
        <span class="text-xs text-muted-foreground">{{ sourceLabel(field) }} · {{ field.valueType }}</span>
      </div>
      <p v-if="field.description" class="text-xs text-muted-foreground">{{ field.description }}</p>
      <p class="text-xs text-muted-foreground">字段键：{{ field.fieldKey }}</p>

      <select
        v-if="field.valueType === 'BOOLEAN'"
        :id="`generation-${field.fieldKey}`"
        :value="values[field.fieldKey]"
        :disabled="disabled || conflictFor(field.fieldKey)?.resolution === null"
        class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
        @change="updateSelect(field.fieldKey, $event)"
      >
        <option value="">请选择</option>
        <option value="true">是（true）</option>
        <option value="false">否（false）</option>
      </select>
      <Input
        v-else
        :id="`generation-${field.fieldKey}`"
        :model-value="values[field.fieldKey]"
        :disabled="disabled || conflictFor(field.fieldKey)?.resolution === null"
        :inputmode="field.valueType === 'DECIMAL' ? 'decimal' : 'text'"
        :placeholder="field.valueType === 'DATE' ? '例如：2026年8月6日 或 2026-08-06' : ''"
        @update:model-value="updateText(field.fieldKey, $event)"
      />
      <p v-if="fieldErrors[field.fieldKey]" role="alert" class="text-sm text-destructive">{{ fieldErrors[field.fieldKey] }}</p>
      <div
        v-if="conflictFor(field.fieldKey)"
        class="space-y-3 rounded-md border border-amber-300 bg-amber-50 p-3 text-sm text-amber-950"
      >
        <p class="font-medium">该字段的案件或系统值在审核后发生了变化，请选择：</p>
        <dl class="grid gap-2 sm:grid-cols-2">
          <div>
            <dt class="text-xs text-amber-800">之前审核的值</dt>
            <dd class="break-all">{{ conflictFor(field.fieldKey)?.previousValue || '（空）' }}</dd>
          </div>
          <div>
            <dt class="text-xs text-amber-800">当前建议值</dt>
            <dd class="break-all">{{ conflictFor(field.fieldKey)?.currentValue || '（空）' }}</dd>
          </div>
        </dl>
        <div class="flex flex-wrap gap-2">
          <Button
            size="sm"
            :variant="conflictFor(field.fieldKey)?.resolution === 'USE_CURRENT' ? 'default' : 'outline'"
            :disabled="disabled"
            @click="emit('resolveConflict', field.fieldKey, 'USE_CURRENT')"
          >
            使用当前案件/系统值
          </Button>
          <Button
            size="sm"
            :variant="conflictFor(field.fieldKey)?.resolution === 'KEEP_PREVIOUS' ? 'default' : 'outline'"
            :disabled="disabled"
            @click="emit('resolveConflict', field.fieldKey, 'KEEP_PREVIOUS')"
          >
            保留之前的值（人工输入）
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>
