<script setup lang="ts">
import { ref, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import SafeIcon from '@/components/common/SafeIcon.vue'
import { downloadTemplateVersion, fetchTemplateVersion, fetchTemplateVersions, presentTemplateError } from '@/services/documentTemplateService'
import type { DocumentTemplateSummary, DocumentTemplateVersionSummary, PublishedTemplateVersion } from '@/types/documentGeneration'

const props = defineProps<{ open: boolean; template: DocumentTemplateSummary | null }>()
const emit = defineEmits<{ close: [] }>()
const versions = ref<DocumentTemplateVersionSummary[]>([])
const selected = ref<PublishedTemplateVersion | null>(null)
const loading = ref(false)
const downloading = ref(false)
const error = ref('')

const load = async () => {
  if (!props.template) return
  loading.value = true
  error.value = ''
  try {
    const page = await fetchTemplateVersions(props.template.id, 0, 100)
    versions.value = page.items
    if (page.items[0]) selected.value = await fetchTemplateVersion(props.template.id, page.items[0].versionNumber)
  } catch (cause) {
    error.value = presentTemplateError(cause, '模板版本加载失败').detail ?? '模板版本加载失败，请重试。'
  } finally {
    loading.value = false
  }
}

watch(() => props.open, open => {
  if (open) void load()
  else {
    versions.value = []
    selected.value = null
    error.value = ''
  }
})

const choose = async (versionNumber: number) => {
  if (!props.template) return
  loading.value = true
  try {
    selected.value = await fetchTemplateVersion(props.template.id, versionNumber)
  } catch (cause) {
    error.value = presentTemplateError(cause, '模板版本读取失败').detail ?? '模板版本读取失败。'
  } finally {
    loading.value = false
  }
}

const download = async () => {
  if (!selected.value || downloading.value) return
  downloading.value = true
  error.value = ''
  try {
    const blob = await downloadTemplateVersion(selected.value.templateId, selected.value.versionNumber)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = selected.value.originalFileName
    link.click()
    URL.revokeObjectURL(url)
  } catch (cause) {
    error.value = presentTemplateError(cause, '模板下载失败').detail ?? '模板下载失败，请重试。'
  } finally {
    downloading.value = false
  }
}

const generationHref = () => selected.value
  ? `./document-generate-selection.html?templateId=${selected.value.templateId}&versionNumber=${selected.value.versionNumber}`
  : '#'
</script>

<template>
  <Dialog :open="open" @update:open="emit('close')">
    <DialogContent class="flex max-h-[90vh] max-w-3xl flex-col overflow-auto">
      <DialogHeader>
        <DialogTitle>{{ template?.name || '模板版本' }}</DialogTitle>
        <DialogDescription>{{ template?.description || '查看不可变版本及其字段配置' }}</DialogDescription>
      </DialogHeader>
      <p v-if="error" role="alert" class="rounded-md bg-destructive/10 p-3 text-sm text-destructive">{{ error }}</p>
      <div v-if="loading && versions.length === 0" class="flex items-center justify-center gap-2 py-10 text-sm text-muted-foreground">
        <SafeIcon name="LoaderCircle" :size="18" class="animate-spin" />正在加载版本...
      </div>
      <template v-else>
        <div class="flex flex-wrap gap-2">
          <Button
            v-for="version in versions"
            :key="version.versionNumber"
            size="sm"
            :variant="selected?.versionNumber === version.versionNumber ? 'default' : 'outline'"
            :disabled="loading"
            @click="choose(version.versionNumber)"
          >版本 {{ version.versionNumber }}</Button>
          <p v-if="versions.length === 0" class="text-sm text-muted-foreground">暂无已发布版本</p>
        </div>
        <div v-if="selected" class="space-y-4 rounded-lg border p-4">
          <div>
            <p class="font-medium">版本 {{ selected.versionNumber }} · {{ selected.originalFileName }}</p>
            <p class="text-xs text-muted-foreground">{{ selected.fileSize }} bytes · SHA-256 {{ selected.contentSha256.slice(0, 12) }}…</p>
          </div>
          <div class="space-y-2">
            <p class="text-sm font-medium">字段配置（{{ selected.fields.length }}）</p>
            <div v-if="selected.fields.length" class="space-y-2">
              <div v-for="field in selected.fields" :key="field.fieldKey" class="rounded-md bg-muted p-3 text-sm">
                <p class="font-medium">{{ field.displayName }} <span class="font-mono text-xs text-muted-foreground">{{ field.fieldKey }}</span></p>
                <p class="text-xs text-muted-foreground">{{ field.valueType }} · {{ field.required ? '必填' : '选填' }} · {{ field.defaultSource }}{{ field.sourceKey ? `/${field.sourceKey}` : '' }}</p>
              </div>
            </div>
            <p v-else class="text-sm text-muted-foreground">此版本没有占位字段。</p>
          </div>
          <div class="flex flex-wrap justify-end gap-2">
            <Button variant="outline" :disabled="downloading" @click="download">{{ downloading ? '下载中...' : '下载已发布 DOCX' }}</Button>
            <Button as="a" :href="generationHref()">使用此版本生成文书</Button>
          </div>
        </div>
      </template>
    </DialogContent>
  </Dialog>
</template>
