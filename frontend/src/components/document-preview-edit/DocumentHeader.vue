
<script setup lang="ts">
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'
import type { DocumentModel } from '@/data/document'
import type { CaseModel } from '@/data/case'
import type { UserModel } from '@/data/user'

interface Props {
  document: DocumentModel
  case: CaseModel
  leadAttorney?: UserModel | null
}

defineProps<Props>()

const formatDate = (dateStr: string) => {
  if (!dateStr) return '未设置'
  try {
    const date = new Date(dateStr)
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return dateStr
  }
}
</script>

<template>
  <Card>
    <CardHeader>
      <CardTitle class="text-base">文书信息</CardTitle>
    </CardHeader>
    <CardContent class="space-y-4">
      <!-- 文书基本信息 -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <p class="text-sm text-muted-foreground">文书名称</p>
          <p class="font-medium">{{ document.docName }}</p>
        </div>
        <div>
          <p class="text-sm text-muted-foreground">生成时间</p>
          <p class="font-medium">{{ formatDate(document.generatedDate) }}</p>
        </div>
      </div>

      <!-- 案件关联信息 -->
      <div class="border-t pt-4 space-y-4">
        <h3 class="font-semibold text-sm">关联案件</h3>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <p class="text-sm text-muted-foreground">案号</p>
            <p class="font-medium">{{ caseData.caseNumber }}</p>
          </div>
          <div>
            <p class="text-sm text-muted-foreground">法院</p>
            <p class="font-medium">{{ caseData.courtName }}</p>
          </div>
          <div>
            <p class="text-sm text-muted-foreground">案由</p>
            <p class="font-medium">{{ caseData.caseCause }}</p>
          </div>
          <div>
            <p class="text-sm text-muted-foreground">案件阶段</p>
            <Badge variant="outline">{{ document.stage }}</Badge>
          </div>
          <div>
            <p class="text-sm text-muted-foreground">原告</p>
            <p class="font-medium">{{ caseData.plaintiff }}</p>
          </div>
          <div>
            <p class="text-sm text-muted-foreground">被告</p>
            <p class="font-medium">{{ caseData.defendant }}</p>
          </div>
        </div>
      </div>

      <!-- 主办律师信息 -->
      <div class="border-t pt-4">
        <p class="text-sm text-muted-foreground mb-2">主办律师</p>
        <div class="flex items-center gap-3 p-3 bg-muted rounded-lg">
          <div class="flex h-10 w-10 items-center justify-center rounded-full bg-primary text-primary-foreground">
            <SafeIcon name="User" :size="16" />
          </div>
          <div>
            <p class="font-medium text-sm">{{ leadAttorney?.name || '未知' }}</p>
            <p class="text-xs text-muted-foreground">{{ leadAttorney?.email || '无邮箱' }}</p>
          </div>
        </div>
      </div>
    </CardContent>
  </Card>
</template>
