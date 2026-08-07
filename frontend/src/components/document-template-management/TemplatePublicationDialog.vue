<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'
import SafeIcon from '@/components/common/SafeIcon.vue'
import {
  CASE_SOURCE_OPTIONS,
  SYSTEM_SOURCE_OPTIONS,
  buildFields,
  initialPublicationDrafts,
  inspectTemplate,
  markerId,
  presentTemplateError,
  publishTemplate,
  publishTemplateVersion,
  validatePublication,
} from '@/services/documentTemplateService'
import type { DocumentTemplateSummary, PublishedTemplateVersion } from '@/types/documentGeneration'
import type { DetectedTemplateMarker, TemplatePublicationDraft } from '@/types/documentTemplate'

const props = defineProps<{ open: boolean; template: DocumentTemplateSummary | null }>()
const emit = defineEmits<{ close: []; published: [version: PublishedTemplateVersion] }>()
const step = ref<'file' | 'mapping' | 'review' | 'success'>('file')
const name = ref('')
const description = ref('')
const file = ref<File | null>(null)
const markers = ref<DetectedTemplateMarker[]>([])
const drafts = ref<TemplatePublicationDraft[]>([])
const assignments = ref<Record<string, string>>({})
const fieldErrors = ref<Record<string, Record<string, string>>>({})
const formError = ref('')
const error = ref('')
const busy = ref(false)
const published = ref<PublishedTemplateVersion | null>(null)

watch(() => props.open, open => {
  if (!open) return
  step.value = 'file'
  name.value = ''
  description.value = ''
  file.value = null
  markers.value = []
  drafts.value = []
  assignments.value = {}
  fieldErrors.value = {}
  formError.value = ''
  error.value = ''
  published.value = null
})

const title = computed(() => props.template ? `为“${props.template.name}”发布新版本` : '创建自定义模板')
const activeDrafts = computed(() => {
  const ids = new Set(Object.values(assignments.value))
  return drafts.value.filter(draft => ids.has(draft.clientId))
})

const onFile = (event: Event) => {
  file.value = (event.target as HTMLInputElement).files?.[0] ?? null
}

const inspect = async () => {
  if (!file.value || (!props.template && !name.value.trim()) || busy.value) {
    error.value = !file.value ? '请选择 DOCX 文件。' : '请输入模板名称。'
    return
  }
  busy.value = true
  error.value = ''
  try {
    const result = await inspectTemplate(file.value)
    markers.value = result.markers
    const initial = initialPublicationDrafts(result.markers)
    drafts.value = initial.drafts
    assignments.value = initial.assignments
    step.value = 'mapping'
  } catch (cause) {
    const presentation = presentTemplateError(cause, '模板检查失败')
    error.value = presentation.detail ?? presentation.summary
  } finally {
    busy.value = false
  }
}

const sourceOptions = (draft: TemplatePublicationDraft) => draft.defaultSource === 'CASE_FIELD'
  ? CASE_SOURCE_OPTIONS.filter(option => option.type === draft.valueType)
  : SYSTEM_SOURCE_OPTIONS.filter(option => option.type === draft.valueType)

const changeSource = (draft: TemplatePublicationDraft, value: string) => {
  draft.defaultSource = value as TemplatePublicationDraft['defaultSource']
  draft.sourceKey = ''
}

const canAssign = (marker: DetectedTemplateMarker, draft: TemplatePublicationDraft) => {
  if (marker.kind === 'CANONICAL') return draft.fieldKey === marker.value
  const canonical = markers.value.find(item => item.kind === 'CANONICAL'
    && assignments.value[markerId(item)] === draft.clientId)
  return !canonical || draft.fieldKey === canonical.value
}

const fieldGroupLabel = (draft: TemplatePublicationDraft) => {
  const displayName = draft.displayName || draft.fieldKey || draft.clientId
  return draft.fieldKey ? `${displayName}（${draft.fieldKey}）` : displayName
}

const review = () => {
  const validation = validatePublication(markers.value, drafts.value, assignments.value)
  fieldErrors.value = validation.fieldErrors
  formError.value = validation.formError ?? ''
  if (validation.valid) step.value = 'review'
}

const submit = async () => {
  if (!file.value || busy.value) return
  busy.value = true
  error.value = ''
  try {
    const fields = buildFields(markers.value, drafts.value, assignments.value)
    published.value = props.template
      ? await publishTemplateVersion(props.template.id, file.value, { fields })
      : await publishTemplate(file.value, {
          name: name.value.trim(), description: description.value.trim() || null, fields,
        })
    step.value = 'success'
    emit('published', published.value)
  } catch (cause) {
    const presentation = presentTemplateError(cause, '模板发布失败')
    error.value = presentation.detail ?? presentation.summary
  } finally {
    busy.value = false
  }
}

const generationHref = computed(() => published.value
  ? `./document-generate-selection.html?templateId=${published.value.templateId}&versionNumber=${published.value.versionNumber}`
  : '#')
</script>

<template>
  <Dialog :open="open" @update:open="emit('close')">
    <DialogContent class="flex max-h-[92vh] max-w-4xl flex-col overflow-auto">
      <TooltipProvider :delay-duration="200">
      <DialogHeader>
        <DialogTitle>{{ title }}</DialogTitle>
        <DialogDescription>上传受控 DOCX，检查并确认字段映射后发布不可变版本。</DialogDescription>
      </DialogHeader>
      <p v-if="error" role="alert" class="rounded-md bg-destructive/10 p-3 text-sm text-destructive">{{ error }}</p>

      <div v-if="step === 'file'" class="space-y-4">
        <template v-if="!template">
          <label class="block space-y-1 text-sm">模板名称 *<Input v-model="name" maxlength="200" /></label>
          <label class="block space-y-1 text-sm">模板描述<Input v-model="description" maxlength="1000" /></label>
        </template>
        <div v-else class="rounded-md bg-muted p-3 text-sm">模板身份保持不变：{{ template.name }}</div>
        <div class="space-y-1 text-sm">
          <span class="flex items-center gap-1">受控 DOCX 文件 *
            <Tooltip>
              <TooltipTrigger as-child>
                <button type="button" aria-label="查看 DOCX 占位符语法说明" class="rounded-full text-muted-foreground hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
                  <SafeIcon name="CircleHelp" :size="15" />
                </button>
              </TooltipTrigger>
              <TooltipContent class="max-w-sm space-y-1 leading-relaxed">
                <p>支持规范标记和中文标记。</p>
                <p>规范标记：<span v-pre class="font-mono">{{case_number}}</span>。以小写字母开头，只能包含小写字母、数字和下划线，最长 100 个字符。</p>
                <p>中文标记：<span v-pre class="font-mono">{{案件编号}}</span>。只能包含 1–40 个汉字。</p>
                <p>花括号内不能包含空格。</p>
              </TooltipContent>
            </Tooltip>
          </span>
          <input aria-label="受控 DOCX 文件" type="file" accept=".docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document" :disabled="busy" @change="onFile">
        </div>
        <div class="flex justify-end gap-2">
          <Button variant="outline" @click="emit('close')">取消</Button>
          <Button :disabled="busy" @click="inspect">{{ busy ? '检查中...' : '检查 DOCX' }}</Button>
        </div>
      </div>

      <div v-else-if="step === 'mapping'" class="space-y-5">
        <p class="text-sm text-muted-foreground">检测到 {{ markers.length }} 个唯一标记。每个标记必须映射一次；规范标记必须保留自身字段键。</p>
        <div v-if="markers.length" class="space-y-2 rounded-lg border p-3">
          <div v-for="marker in markers" :key="markerId(marker)" class="grid gap-2 rounded-md bg-muted p-3 md:grid-cols-[1fr_1fr]">
            <div class="text-sm"><span class="font-mono">{{ marker.value }}</span> · {{ marker.kind === 'CANONICAL' ? '规范标记' : '中文标记' }} · {{ marker.occurrenceCount }} 处</div>
            <div class="space-y-1">
              <span class="flex items-center gap-1 text-xs text-muted-foreground">映射到字段组
                <Tooltip>
                  <TooltipTrigger as-child>
                    <button type="button" aria-label="查看字段组说明" class="rounded-full hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
                      <SafeIcon name="CircleHelp" :size="14" />
                    </button>
                  </TooltipTrigger>
                  <TooltipContent class="max-w-sm leading-relaxed">
                    字段组让含义相同的多个占位符共享一个字段值。例如 <span v-pre class="font-mono">{{案号}}</span> 和 <span v-pre class="font-mono">{{案件编号}}</span> 可以共同映射到 <span class="font-mono">case_number</span>，生成时只需提供一次案件编号。
                  </TooltipContent>
                </Tooltip>
              </span>
              <select v-model="assignments[markerId(marker)]" :aria-label="`为 ${marker.value} 选择字段组`" :disabled="marker.kind === 'CANONICAL'" class="h-9 w-full rounded-md border bg-background px-2 text-sm">
                <option v-for="draft in drafts" :key="draft.clientId" :value="draft.clientId" :disabled="!canAssign(marker, draft)">映射到字段组：{{ fieldGroupLabel(draft) }}</option>
              </select>
            </div>
          </div>
        </div>

        <div v-for="draft in activeDrafts" :key="draft.clientId" class="space-y-3 rounded-lg border p-4">
          <div class="grid gap-3 md:grid-cols-2">
            <div class="space-y-1 text-sm">
              <span class="flex items-center gap-1">字段键 *
                <Tooltip>
                  <TooltipTrigger as-child>
                    <button type="button" aria-label="查看字段键说明" class="rounded-full text-muted-foreground hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
                      <SafeIcon name="CircleHelp" :size="14" />
                    </button>
                  </TooltipTrigger>
                  <TooltipContent class="max-w-sm space-y-1 leading-relaxed">
                    <p>字段键是生成文书时识别字段的稳定名称，例如 <span class="font-mono">case_number</span>。</p>
                    <p>必须以小写字母开头，只能包含小写字母、数字和下划线。中文标记需要手动填写；规范标记的字段键不能修改。</p>
                  </TooltipContent>
                </Tooltip>
              </span>
              <Input v-model="draft.fieldKey" :aria-label="`${draft.displayName || '未命名字段'}的字段键`" :disabled="markers.some(marker => marker.kind === 'CANONICAL' && assignments[markerId(marker)] === draft.clientId)" />
            </div>
            <label class="space-y-1 text-sm">显示名称 *<Input v-model="draft.displayName" /></label>
            <label class="space-y-1 text-sm">字段类型
              <select v-model="draft.valueType" class="flex h-10 w-full rounded-md border bg-background px-3 text-sm" @change="draft.sourceKey = ''">
                <option value="TEXT">TEXT</option><option value="DATE">DATE</option><option value="DECIMAL">DECIMAL</option><option value="BOOLEAN">BOOLEAN</option>
              </select>
            </label>
            <label class="space-y-1 text-sm">默认来源
              <select :value="draft.defaultSource" class="flex h-10 w-full rounded-md border bg-background px-3 text-sm" @change="changeSource(draft, ($event.target as HTMLSelectElement).value)">
                <option value="USER_INPUT">手动输入</option><option value="CASE_FIELD">案件字段</option><option value="SYSTEM_VALUE">系统值</option>
              </select>
            </label>
            <label v-if="draft.defaultSource !== 'USER_INPUT'" class="space-y-1 text-sm">来源字段
              <select v-model="draft.sourceKey" class="flex h-10 w-full rounded-md border bg-background px-3 text-sm">
                <option value="">请选择</option><option v-for="option in sourceOptions(draft)" :key="option.key" :value="option.key">{{ option.label }}（{{ option.key }}）</option>
              </select>
            </label>
            <label class="flex items-center gap-2 self-end text-sm"><input v-model="draft.required" type="checkbox">必填字段</label>
          </div>
          <label class="block space-y-1 text-sm">字段说明<Input v-model="draft.description" /></label>
          <div v-if="fieldErrors[draft.clientId]" class="text-sm text-destructive">{{ Object.values(fieldErrors[draft.clientId]).join(' ') }}</div>
        </div>
        <p v-if="markers.length === 0" class="rounded-md bg-muted p-4 text-sm">此 DOCX 没有占位标记，将发布空字段合同。</p>
        <p v-if="formError" role="alert" class="text-sm text-destructive">{{ formError }}</p>
        <div class="flex justify-between"><Button variant="outline" @click="step = 'file'">返回选择文件</Button><Button @click="review">审核发布内容</Button></div>
      </div>

      <div v-else-if="step === 'review'" class="space-y-4">
        <div class="rounded-md bg-muted p-4 text-sm">
          <p class="font-medium">{{ template?.name || name }} · {{ file?.name }}</p>
          <p>{{ activeDrafts.length }} 个字段 · {{ markers.length }} 个唯一标记</p>
        </div>
        <div v-for="field in buildFields(markers, drafts, assignments)" :key="field.fieldKey" class="rounded-md border p-3 text-sm">
          <p class="font-medium">{{ field.displayName }}（{{ field.fieldKey }}）</p>
          <p class="text-muted-foreground">{{ field.valueType }} · {{ field.required ? '必填' : '选填' }} · {{ field.defaultSource }}{{ field.sourceKey ? `/${field.sourceKey}` : '' }}</p>
          <p class="font-mono text-xs">{{ field.markers.map(marker => marker.value).join('、') }}</p>
        </div>
        <div class="flex justify-between"><Button variant="outline" :disabled="busy" @click="step = 'mapping'">返回修改</Button><Button :disabled="busy" @click="submit">{{ busy ? '发布中...' : '确认并发布' }}</Button></div>
      </div>

      <div v-else class="space-y-4 text-center">
        <SafeIcon name="CircleCheck" :size="48" class="mx-auto text-green-700" />
        <div><p class="font-semibold">模板版本发布成功</p><p class="text-sm text-muted-foreground">版本 {{ published?.versionNumber }} · {{ published?.originalFileName }}</p></div>
        <div class="flex justify-center gap-2"><Button variant="outline" @click="emit('close')">返回模板管理</Button><Button as="a" :href="generationHref">使用此版本生成文书</Button></div>
      </div>
      </TooltipProvider>
    </DialogContent>
  </Dialog>
</template>
