<script setup lang="ts">
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import SafeIcon from '@/components/common/SafeIcon.vue'
import type {
  DocumentTemplateSummary,
  DocumentTemplateVersionSummary,
  PublishedTemplateVersion,
} from '@/types/documentGeneration'

defineProps<{
  templates: DocumentTemplateSummary[]
  versions: DocumentTemplateVersionSummary[]
  selectedTemplateId: number | null
  selectedVersionNumber: number | null
  exactVersion: PublishedTemplateVersion | null
  isLoadingTemplates: boolean
  isLoadingVersions: boolean
  isLoadingExactVersion: boolean
  templateError: string
  versionError: string
  hasMoreTemplates: boolean
  hasMoreVersions: boolean
  disabled: boolean
}>()

const emit = defineEmits<{
  selectTemplate: [templateId: number]
  selectVersion: [versionNumber: number]
  loadMoreTemplates: []
  loadMoreVersions: []
  retryTemplates: []
  retryVersions: []
}>()

const formatDate = (value: string) => new Date(value).toLocaleDateString('zh-CN')
</script>

<template>
  <div class="space-y-5">
    <section class="space-y-2">
      <h4 class="text-sm font-medium">模板</h4>
      <div v-if="isLoadingTemplates && templates.length === 0" class="flex items-center justify-center gap-2 rounded-lg border py-12 text-sm text-muted-foreground">
        <SafeIcon name="LoaderCircle" :size="18" class="animate-spin" />
        正在加载模板...
      </div>
      <div v-else class="max-h-52 space-y-2 overflow-auto rounded-lg border p-3">
        <button
          v-for="template in templates"
          :key="template.id"
          type="button"
          :disabled="disabled"
          class="w-full rounded-md border p-3 text-left hover:bg-accent disabled:cursor-not-allowed disabled:opacity-60"
          :class="selectedTemplateId === template.id ? 'border-primary bg-primary/10' : ''"
          @click="emit('selectTemplate', template.id)"
        >
          <div class="flex items-start justify-between gap-2">
            <div class="min-w-0">
              <p class="truncate text-sm font-semibold">{{ template.name }}</p>
              <p class="mt-1 line-clamp-2 text-xs text-muted-foreground">{{ template.description || '暂无描述' }}</p>
            </div>
            <Badge variant="outline">{{ template.templateType === 'CUSTOM' ? '自定义' : '系统预设' }}</Badge>
          </div>
        </button>
        <p v-if="templates.length === 0 && !templateError" class="py-8 text-center text-sm text-muted-foreground">暂无已发布模板</p>
      </div>
      <p v-if="templateError" role="alert" class="text-sm text-destructive">{{ templateError }}</p>
      <Button v-if="templateError" variant="outline" size="sm" :disabled="disabled" @click="emit('retryTemplates')">重试加载模板</Button>
      <Button v-else-if="hasMoreTemplates" variant="outline" size="sm" :disabled="isLoadingTemplates || disabled" @click="emit('loadMoreTemplates')">
        {{ isLoadingTemplates ? '加载中...' : '加载更多模板' }}
      </Button>
    </section>

    <section v-if="selectedTemplateId !== null" class="space-y-2">
      <h4 class="text-sm font-medium">模板版本</h4>
      <div v-if="isLoadingVersions && versions.length === 0" class="flex items-center gap-2 text-sm text-muted-foreground">
        <SafeIcon name="LoaderCircle" :size="16" class="animate-spin" />
        正在加载版本...
      </div>
      <div v-else class="flex flex-wrap gap-2">
        <Button
          v-for="version in versions"
          :key="version.versionNumber"
          size="sm"
          :disabled="disabled"
          :variant="selectedVersionNumber === version.versionNumber ? 'default' : 'outline'"
          @click="emit('selectVersion', version.versionNumber)"
        >
          版本 {{ version.versionNumber }} · {{ formatDate(version.publishedAt) }}
        </Button>
      </div>
      <p v-if="versions.length === 0 && !isLoadingVersions && !versionError" class="text-sm text-muted-foreground">该模板暂无已发布版本</p>
      <p v-if="versionError" role="alert" class="text-sm text-destructive">{{ versionError }}</p>
      <Button v-if="versionError" variant="outline" size="sm" :disabled="disabled" @click="emit('retryVersions')">重试加载版本</Button>
      <Button v-else-if="hasMoreVersions" variant="outline" size="sm" :disabled="isLoadingVersions || disabled" @click="emit('loadMoreVersions')">
        {{ isLoadingVersions ? '加载中...' : '加载更多版本' }}
      </Button>
    </section>

    <div v-if="isLoadingExactVersion" class="flex items-center gap-2 text-sm text-muted-foreground">
      <SafeIcon name="LoaderCircle" :size="16" class="animate-spin" />
      正在读取所选版本...
    </div>
    <div v-else-if="exactVersion" class="rounded-lg bg-muted p-3 text-sm">
      <p class="font-medium">{{ exactVersion.templateName }} · 版本 {{ exactVersion.versionNumber }}</p>
      <p class="mt-1 text-muted-foreground">{{ exactVersion.templateDescription || '暂无版本描述' }}</p>
      <p class="mt-2 text-xs text-muted-foreground">{{ exactVersion.originalFileName }} · {{ exactVersion.fields.length }} 个字段</p>
    </div>
  </div>
</template>
