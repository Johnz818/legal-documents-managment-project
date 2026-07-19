
<script setup lang="ts">
import { ref } from 'vue'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import SafeIcon from '@/components/common/SafeIcon.vue'

// Navigation entries for the bulk management hub
const navigationEntries = ref([
  {
    id: 'case_list',
    title: '查看案件列表',
    description: '浏览所有案件，支持筛选、排序和批量操作',
    icon: 'FolderOpen',
    href: './case-list-view.html',
    color: 'bg-blue-50 dark:bg-blue-950',
    iconColor: 'text-blue-600 dark:text-blue-400',
  },
  {
    id: 'case_create',
    title: '手动创建案件',
    description: '填写表单快速创建单个新案件',
    icon: 'FilePlus',
    href: './case-create-manual.html',
    color: 'bg-green-50 dark:bg-green-950',
    iconColor: 'text-green-600 dark:text-green-400',
  },
  {
    id: 'case_import',
    title: '批量导入案件',
    description: '通过Excel文件批量导入多个案件',
    icon: 'Upload',
    href: './case-import-batch.html',
    color: 'bg-purple-50 dark:bg-purple-950',
    iconColor: 'text-purple-600 dark:text-purple-400',
  },
  {
    id: 'case_tags',
    title: '管理案件标签',
    description: '创建和编辑自定义标签，灵活分类案件',
    icon: 'Tags',
    href: './case-tag-management.html',
    color: 'bg-orange-50 dark:bg-orange-950',
    iconColor: 'text-orange-600 dark:text-orange-400',
  },
])
</script>

<template>
  <div class="flex flex-col h-full">
    <!-- Page Header -->
    <div class="border-b px-6 py-6">
      <div class="max-w-7xl mx-auto">
        <h1 class="text-3xl font-bold tracking-tight">案件批量管理</h1>
        <p class="mt-2 text-muted-foreground">
          集中管理所有诉讼案件，支持创建、导入、筛选和标签管理
        </p>
      </div>
    </div>

    <!-- Main Content -->
    <div class="flex-1 overflow-auto">
      <div class="px-6 py-8">
        <div class="max-w-7xl mx-auto">
          <!-- Navigation Cards Grid -->
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <a
              v-for="entry in navigationEntries"
              :key="entry.id"
              :href="entry.href"
              class="group"
            >
              <Card class="h-full transition-all hover:shadow-lg hover:border-primary/50 cursor-pointer">
                <CardHeader>
                  <div :class="[entry.color, 'w-12 h-12 rounded-lg flex items-center justify-center mb-4 group-hover:scale-110 transition-transform']">
                    <SafeIcon :name="entry.icon" :size="24" :class="entry.iconColor" />
                  </div>
                  <CardTitle class="text-lg">{{ entry.title }}</CardTitle>
                  <CardDescription class="text-sm">{{ entry.description }}</CardDescription>
                </CardHeader>
                <CardContent>
                  <Button variant="ghost" class="w-full justify-start text-primary group-hover:translate-x-1 transition-transform">
                    进入
                    <SafeIcon name="ArrowRight" :size="16" class="ml-2" />
                  </Button>
                </CardContent>
              </Card>
            </a>
          </div>

          <!-- Quick Stats Section -->
          <div class="mt-12 grid grid-cols-1 md:grid-cols-3 gap-6">
            <Card>
              <CardHeader class="pb-3">
                <CardTitle class="text-sm font-medium text-muted-foreground">总案件数</CardTitle>
              </CardHeader>
              <CardContent>
                <div class="text-3xl font-bold">128</div>
                <p class="text-xs text-muted-foreground mt-1">较上月增加 12 个</p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader class="pb-3">
                <CardTitle class="text-sm font-medium text-muted-foreground">进行中</CardTitle>
              </CardHeader>
              <CardContent>
                <div class="text-3xl font-bold">45</div>
                <p class="text-xs text-muted-foreground mt-1">需要关注的案件</p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader class="pb-3">
                <CardTitle class="text-sm font-medium text-muted-foreground">待处理提醒</CardTitle>
              </CardHeader>
              <CardContent>
                <div class="text-3xl font-bold">8</div>
                <p class="text-xs text-muted-foreground mt-1">
                  <a href="./reminder-dashboard.html" class="text-primary hover:underline">查看详情</a>
                </p>
              </CardContent>
            </Card>
          </div>

          <!-- Quick Links Section -->
          <div class="mt-12 p-6 bg-muted/50 rounded-lg border">
            <h2 class="text-lg font-semibold mb-4">快速链接</h2>
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              <a
                href="./document-generation-entry.html"
                class="flex items-center gap-3 p-3 rounded-lg hover:bg-background transition-colors"
              >
                <SafeIcon name="FileText" :size="20" class="text-primary" />
                <span class="text-sm font-medium">文书自动生成</span>
              </a>
              <a
                href="./reminder-dashboard.html"
                class="flex items-center gap-3 p-3 rounded-lg hover:bg-background transition-colors"
              >
                <SafeIcon name="Bell" :size="20" class="text-primary" />
                <span class="text-sm font-medium">关键环节提醒</span>
              </a>
              <a
                href="./case-calendar-view.html"
                class="flex items-center gap-3 p-3 rounded-lg hover:bg-background transition-colors"
              >
                <SafeIcon name="Calendar" :size="20" class="text-primary" />
                <span class="text-sm font-medium">案件日历视图</span>
              </a>
              <a
                href="./user-management-dashboard.html"
                class="flex items-center gap-3 p-3 rounded-lg hover:bg-background transition-colors"
              >
                <SafeIcon name="Users" :size="20" class="text-primary" />
                <span class="text-sm font-medium">团队协作管理</span>
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Smooth transitions for interactive elements */
:deep(.group) {
  transition: all 0.3s ease;
}
</style>
