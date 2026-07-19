
<script setup lang="ts">
import { computed } from 'vue'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'

interface Props {
  templateName: string
  templateContent: string
  templateDescription?: string
}

const props = defineProps<Props>()

const hasContent = computed(() => props.templateContent.trim().length > 0)

const previewContent = computed(() => {
  if (!hasContent.value) {
    return '（暂无内容）'
  }
  // 将占位符高亮显示
  return props.templateContent.replace(
    /\{\{([^}]+)\}\}/g,
    '<span class="bg-yellow-100 dark:bg-yellow-900 px-1 rounded font-mono text-sm">{{$1}}</span>'
  )
})
</script>

<template>
  <div class="space-y-6">
    <!-- 模板信息卡片 -->
    <Card>
      <CardHeader>
        <div class="flex items-start justify-between">
          <div>
            <CardTitle>{{ templateName || '（未命名模板）' }}</CardTitle>
            <CardDescription v-if="templateDescription" class="mt-2">
              {{ templateDescription }}
            </CardDescription>
          </div>
          <Badge variant="outline">预览</Badge>
        </div>
      </CardHeader>
    </Card>

    <!-- 内容预览 -->
    <Card>
      <CardHeader>
        <CardTitle class="text-base">模板内容预览</CardTitle>
        <CardDescription>
          <span v-if="!hasContent" class="text-amber-600 dark:text-amber-400">
            <SafeIcon name="AlertCircle" :size="16" class="inline mr-1" />
            模板内容为空
          </span>
          <span v-else class="text-green-600 dark:text-green-400">
            <SafeIcon name="CheckCircle" :size="16" class="inline mr-1" />
            模板内容已准备好
          </span>
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div class="bg-muted rounded-lg p-6 min-h-96 whitespace-pre-wrap font-mono text-sm leading-relaxed">
          <div v-if="hasContent" v-html="previewContent" class="text-foreground"></div>
          <div v-else class="text-muted-foreground italic">
            在编辑器中输入模板内容，预览将实时更新
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- 占位符说明 -->
    <Card>
      <CardHeader>
        <CardTitle class="text-base">占位符说明</CardTitle>
      </CardHeader>
      <CardContent>
        <div class="space-y-3 text-sm">
          <div class="flex items-start gap-3">
            <div class="bg-yellow-100 dark:bg-yellow-900 px-2 py-1 rounded font-mono text-xs whitespace-nowrap">
              {{占位符}}
            </div>
            <p class="text-muted-foreground">
              以上述格式表示的文本将在生成文书时被实际数据替换
            </p>
          </div>
          <div class="bg-blue-50 dark:bg-blue-950 rounded-md p-3 space-y-2">
            <p class="font-semibold text-blue-900 dark:text-blue-100">常用占位符：</p>
            <ul class="list-disc list-inside space-y-1 text-blue-800 dark:text-blue-200">
              <li>{{案号}} - 案件编号</li>
              <li>{{原告}} - 原告或申请人</li>
              <li>{{被告}} - 被告或被申请人</li>
              <li>{{法院}} - 受理法院</li>
              <li>{{主办律师}} - 负责律师</li>
              <li>{{当前日期}} - 文书生成日期</li>
            </ul>
          </div>
        </div>
      </CardContent>
    </Card>
  </div>
</template>
