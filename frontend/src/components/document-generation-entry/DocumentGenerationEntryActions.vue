
<script setup lang="ts">
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import SafeIcon from '@/components/common/SafeIcon.vue'

const actions = [
  {
    id: 'template_management',
    title: '文书模板管理',
    description: '查看系统预设模板，创建和编辑自定义模板',
    icon: 'LayoutTemplate',
    href: './document-template-management.html',
    features: [
      '系统预设模板库',
      '自定义模板创建',
      '模板编辑与预览',
      '占位符变量管理',
    ],
  },
  {
    id: 'document_generation',
    title: '发起文书生成',
    description: '选择案件和模板，自动生成文书初稿',
    icon: 'FileSignature',
    href: './document-generate-selection.html',
    features: [
      '案件信息自动填充',
      '文书在线编辑',
      '多格式导出',
      '文书保存管理',
    ],
  },
]
</script>

<template>
  <div class="grid gap-6 md:grid-cols-2">
    <Card
      v-for="action in actions"
      :key="action.id"
      class="hover:shadow-lg transition-shadow cursor-pointer"
    >
      <CardHeader>
        <div class="flex items-start justify-between">
          <div class="flex-1">
            <CardTitle class="flex items-center gap-2">
              <SafeIcon :name="action.icon" :size="24" class="text-primary" />
              {{ action.title }}
            </CardTitle>
            <CardDescription class="mt-2">{{ action.description }}</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <div class="space-y-4">
          <!-- Features List -->
          <div class="space-y-2">
            <p class="text-sm font-medium text-foreground">主要功能：</p>
            <ul class="space-y-1">
              <li v-for="feature in action.features" :key="feature" class="flex items-center gap-2 text-sm text-muted-foreground">
                <SafeIcon name="Check" :size="16" class="text-green-600" />
                {{ feature }}
              </li>
            </ul>
          </div>

          <!-- Action Button -->
          <Button as="a" :href="action.href" class="w-full mt-4">
            进入
            <SafeIcon name="ArrowRight" :size="16" class="ml-2" />
          </Button>
        </div>
      </CardContent>
    </Card>
  </div>
</template>
