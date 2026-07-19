
<script setup lang="ts">
import { ref } from 'vue'
import type { DocumentTemplateModel } from '@/data/document'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import SafeIcon from '@/components/common/SafeIcon.vue'
import TemplatePreviewDialog from './TemplatePreviewDialog.vue'
import TemplateEditDialog from './TemplateEditDialog.vue'

interface Props {
  template: DocumentTemplateModel
  isPreset: boolean
}

const props = defineProps<Props>()
const showPreview = ref(false)
const showEdit = ref(false)

const handleDelete = () => {
  if (confirm(`确定要删除模板 "${props.template.name}" 吗？`)) {
    // 实际应用中应调用删除API
    console.log('删除模板:', props.template.templateId)
  }
}
</script>

<template>
  <Card class="flex flex-col hover:shadow-card transition-shadow">
    <CardHeader class="pb-3">
      <div class="flex items-start justify-between gap-2">
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 mb-1">
            <CardTitle class="text-base truncate">{{ template.name }}</CardTitle>
            <Badge variant="outline" class="shrink-0">
              {{ template.type }}
            </Badge>
          </div>
          <CardDescription class="line-clamp-2">
            {{ template.description }}
          </CardDescription>
        </div>
        
        <!-- 操作菜单 -->
        <DropdownMenu>
          <DropdownMenuTrigger as-child>
            <Button variant="ghost" size="icon" class="shrink-0">
              <SafeIcon name="MoreVertical" :size="18" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem @click="showPreview = true">
              <SafeIcon name="Eye" :size="16" class="mr-2" />
              预览
            </DropdownMenuItem>
            <DropdownMenuItem v-if="!isPreset" @click="showEdit = true">
              <SafeIcon name="Edit" :size="16" class="mr-2" />
              编辑
            </DropdownMenuItem>
            <DropdownMenuItem
              v-if="!isPreset"
              @click="handleDelete"
              class="text-destructive"
            >
              <SafeIcon name="Trash2" :size="16" class="mr-2" />
              删除
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </CardHeader>

    <CardContent class="flex-1 pb-3">
      <div class="space-y-2 text-sm">
        <div class="flex items-center gap-2 text-muted-foreground">
          <SafeIcon name="Calendar" :size="16" />
          <span>最后修改: {{ template.lastModified }}</span>
        </div>
        <div v-if="template.creator" class="flex items-center gap-2 text-muted-foreground">
          <SafeIcon name="User" :size="16" />
          <span>创建者: {{ template.creator }}</span>
        </div>
      </div>
    </CardContent>

    <!-- 底部操作按钮 -->
    <div class="border-t p-3 flex gap-2">
      <Button
        variant="outline"
        size="sm"
        class="flex-1"
        @click="showPreview = true"
      >
        <SafeIcon name="Eye" :size="16" class="mr-1" />
        预览
      </Button>
      <Button
        v-if="!isPreset"
        variant="outline"
        size="sm"
        class="flex-1"
        @click="showEdit = true"
      >
        <SafeIcon name="Edit" :size="16" class="mr-1" />
        编辑
      </Button>
      <Button
        v-else
        variant="outline"
        size="sm"
        class="flex-1"
        as="a"
        href="./document-generate-selection.html"
      >
        <SafeIcon name="FileText" :size="16" class="mr-1" />
        使用
      </Button>
    </div>

    <!-- 预览对话框 -->
    <TemplatePreviewDialog
      :open="showPreview"
      :template="template"
      @close="showPreview = false"
    />

    <!-- 编辑对话框 -->
    <TemplateEditDialog
      v-if="!isPreset"
      :open="showEdit"
      :template="template"
      @close="showEdit = false"
    />
  </Card>
</template>
