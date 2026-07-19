
<script setup lang="ts">
import { ref } from 'vue'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import SafeIcon from '@/components/common/SafeIcon.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import DocumentGenerationEntryActions from '@/components/document-generation-entry/DocumentGenerationEntryActions.vue'
import DocumentGenerationEntryTemplatePreview from '@/components/document-generation-entry/DocumentGenerationEntryTemplatePreview.vue'

const breadcrumbs = [
  { label: '首页', href: './case-list-view.html' },
  { label: '文书生成入口' },
]

const recentTemplates = [
  {
    id: 'template_1',
    name: '起诉状',
    description: '民事案件起诉状模板',
    category: '民事诉讼',
    usageCount: 45,
  },
  {
    id: 'template_2',
    name: '答辩状',
    description: '民事案件答辩状模板',
    category: '民事诉讼',
    usageCount: 32,
  },
  {
    id: 'template_3',
    name: '代理词',
    description: '法庭代理词模板',
    category: '民事诉讼',
    usageCount: 28,
  },
  {
    id: 'template_4',
    name: '证据目录',
    description: '案件证据目录模板',
    category: '证据管理',
    usageCount: 56,
  },
]
</script>

<template>
  <div class="flex flex-col h-full">
    <!-- Page Header -->
    <PageHeader
      title="文书生成入口"
      description="快速进入文书模板管理和发起文书生成流程"
      :breadcrumbs="breadcrumbs"
    >
      <template #actions>
        <Button variant="outline" as="a" href="./case-bulk-management-entry.html">
          <SafeIcon name="ArrowLeft" :size="16" class="mr-2" />
          返回案件管理
        </Button>
      </template>
    </PageHeader>

    <!-- Main Content -->
    <div class="flex-1 overflow-auto">
      <div class="container mx-auto px-4 py-8">
        <!-- Hero Section -->
        <div class="mb-12">
          <div class="rounded-lg border bg-gradient-to-br from-primary/5 to-primary/10 p-8">
            <div class="flex items-start justify-between">
              <div class="flex-1">
                <h2 class="text-2xl font-bold mb-2">文书自动生成系统</h2>
                <p class="text-muted-foreground mb-6">
                  基于案件信息自动生成各类诉讼文书，支持模板管理、可视化编辑和多格式导出。
                </p>
                <div class="flex flex-wrap gap-3">
                  <Button as="a" href="./document-template-management.html" size="lg">
                    <SafeIcon name="LayoutTemplate" :size="18" class="mr-2" />
                    文书模板管理
                  </Button>
                  <Button as="a" href="./document-generate-selection.html" variant="outline" size="lg">
                    <SafeIcon name="FileSignature" :size="18" class="mr-2" />
                    发起文书生成
                  </Button>
                </div>
              </div>
              <div class="hidden lg:flex h-32 w-32 items-center justify-center rounded-lg bg-primary/10">
                <SafeIcon name="FileText" :size="64" class="text-primary/40" />
              </div>
            </div>
          </div>
        </div>

        <!-- Main Actions -->
        <div class="mb-12">
          <h3 class="text-lg font-semibold mb-6">核心功能</h3>
          <DocumentGenerationEntryActions />
        </div>

        <!-- Recent Templates -->
        <div>
          <div class="flex items-center justify-between mb-6">
            <h3 class="text-lg font-semibold">常用模板</h3>
            <Button variant="ghost" as="a" href="./document-template-preset-list.html" size="sm">
              查看全部
              <SafeIcon name="ArrowRight" :size="16" class="ml-2" />
            </Button>
          </div>
          <DocumentGenerationEntryTemplatePreview :templates="recentTemplates" />
        </div>

        <!-- Quick Tips -->
        <div class="mt-12">
          <Card>
            <CardHeader>
              <CardTitle class="flex items-center gap-2">
                <SafeIcon name="Lightbulb" :size="20" class="text-yellow-500" />
                使用提示
              </CardTitle>
            </CardHeader>
            <CardContent>
              <ul class="space-y-3 text-sm text-muted-foreground">
                <li class="flex gap-3">
                  <span class="text-primary font-semibold">1.</span>
                  <span>选择合适的文书模板或创建自定义模板</span>
                </li>
                <li class="flex gap-3">
                  <span class="text-primary font-semibold">2.</span>
                  <span>选择案件后，系统将自动填充案件信息</span>
                </li>
                <li class="flex gap-3">
                  <span class="text-primary font-semibold">3.</span>
                  <span>在预览页面进行必要的编辑和调整</span>
                </li>
                <li class="flex gap-3">
                  <span class="text-primary font-semibold">4.</span>
                  <span>导出为Word或PDF格式，保存到案件记录中</span>
                </li>
              </ul>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  </div>
</template>
