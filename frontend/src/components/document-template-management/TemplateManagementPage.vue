<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import SafeIcon from '@/components/common/SafeIcon.vue'
import TemplateListSection from './TemplateListSection.vue'
import TemplatePublicationDialog from './TemplatePublicationDialog.vue'
import TemplateVersionDialog from './TemplateVersionDialog.vue'
import { fetchTemplates, presentTemplateError } from '@/services/documentTemplateService'
import type { DocumentTemplateSummary } from '@/types/documentGeneration'

const PAGE_SIZE = 20
const searchQuery = ref('')
const templates = ref<DocumentTemplateSummary[]>([])
const page = ref(-1)
const totalPages = ref(0)
const loading = ref(false)
const error = ref('')
const showPublication = ref(false)
const publicationTemplate = ref<DocumentTemplateSummary | null>(null)
const showVersions = ref(false)
const versionTemplate = ref<DocumentTemplateSummary | null>(null)

const hasMore = computed(() => page.value + 1 < totalPages.value)
const filtered = computed(() => {
  const query = searchQuery.value.trim().toLocaleLowerCase()
  if (!query) return templates.value
  return templates.value.filter(template => template.name.toLocaleLowerCase().includes(query)
    || template.description?.toLocaleLowerCase().includes(query))
})
const presets = computed(() => filtered.value.filter(template => template.templateType === 'PRESET'))
const customs = computed(() => filtered.value.filter(template => template.templateType === 'CUSTOM'))

const load = async (reset = false) => {
  if (loading.value) return
  loading.value = true
  error.value = ''
  const nextPage = reset ? 0 : page.value + 1
  if (reset) {
    templates.value = []
    page.value = -1
    totalPages.value = 0
  }
  try {
    const result = await fetchTemplates(nextPage, PAGE_SIZE)
    const known = new Set(templates.value.map(template => template.id))
    templates.value.push(...result.items.filter(template => !known.has(template.id)))
    page.value = result.page
    totalPages.value = result.totalPages
  } catch (cause) {
    const presentation = presentTemplateError(cause, '模板列表加载失败')
    error.value = presentation.detail ?? '模板列表加载失败，请重试。'
  } finally {
    loading.value = false
  }
}

const createTemplate = () => {
  publicationTemplate.value = null
  showPublication.value = true
}
const publishVersion = (template: DocumentTemplateSummary) => {
  publicationTemplate.value = template
  showPublication.value = true
}
const viewVersions = (template: DocumentTemplateSummary) => {
  versionTemplate.value = template
  showVersions.value = true
}
const published = async () => {
  await load(true)
}

onMounted(() => void load(true))
</script>

<template>
  <div class="flex flex-col gap-6 p-6">
    <div class="flex flex-col items-start justify-between gap-4 sm:flex-row sm:items-center">
      <div class="w-full flex-1 sm:w-auto">
        <Input v-model="searchQuery" placeholder="搜索已加载的模板名称或描述..." class="w-full" />
        <p class="mt-1 text-xs text-muted-foreground">当前搜索和数量统计仅覆盖已加载的 {{ templates.length }} 个模板。</p>
      </div>
      <Button class="gap-2" @click="createTemplate"><SafeIcon name="Plus" :size="18" />创建自定义模板</Button>
    </div>

    <div v-if="loading && templates.length === 0" class="flex items-center justify-center gap-2 rounded-lg border py-16 text-sm text-muted-foreground">
      <SafeIcon name="LoaderCircle" :size="18" class="animate-spin" />正在加载模板...
    </div>
    <div v-else-if="error && templates.length === 0" class="space-y-3 rounded-lg border p-10 text-center">
      <p role="alert" class="text-sm text-destructive">{{ error }}</p><Button variant="outline" @click="load(true)">重试加载</Button>
    </div>
    <Tabs v-else default-value="all" class="w-full">
      <TabsList class="grid w-full grid-cols-3">
        <TabsTrigger value="all">全部模板 ({{ filtered.length }})</TabsTrigger>
        <TabsTrigger value="preset">系统预设 ({{ presets.length }})</TabsTrigger>
        <TabsTrigger value="custom">自定义 ({{ customs.length }})</TabsTrigger>
      </TabsList>
      <TabsContent value="all" class="space-y-6">
        <TemplateListSection v-if="presets.length" title="系统预设模板" :templates="presets" @view="viewVersions" @publish-version="publishVersion" />
        <TemplateListSection v-if="customs.length" title="用户自定义模板" :templates="customs" @view="viewVersions" @publish-version="publishVersion" />
        <div v-if="filtered.length === 0" class="py-12 text-center text-muted-foreground"><SafeIcon name="Search" :size="48" class="mx-auto mb-4" /><p>未找到匹配的模板</p></div>
      </TabsContent>
      <TabsContent value="preset"><TemplateListSection v-if="presets.length" title="系统预设模板" :templates="presets" @view="viewVersions" @publish-version="publishVersion" /><p v-else class="py-12 text-center text-muted-foreground">暂无预设模板</p></TabsContent>
      <TabsContent value="custom"><TemplateListSection v-if="customs.length" title="用户自定义模板" :templates="customs" @view="viewVersions" @publish-version="publishVersion" /><div v-else class="space-y-3 py-12 text-center text-muted-foreground"><p>暂无自定义模板</p><Button variant="outline" @click="createTemplate">创建第一个模板</Button></div></TabsContent>
    </Tabs>

    <p v-if="error && templates.length" role="alert" class="text-sm text-destructive">{{ error }}</p>
    <Button v-if="hasMore" variant="outline" :disabled="loading" class="self-center" @click="load(false)">{{ loading ? '加载中...' : '加载更多模板' }}</Button>

    <TemplatePublicationDialog :open="showPublication" :template="publicationTemplate" @close="showPublication = false" @published="published" />
    <TemplateVersionDialog :open="showVersions" :template="versionTemplate" @close="showVersions = false" />
  </div>
</template>
