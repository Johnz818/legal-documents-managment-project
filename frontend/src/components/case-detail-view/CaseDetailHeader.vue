
<script setup lang="ts">
import { computed } from 'vue'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import type { CaseDetailResponse } from '@/types/case'

interface Props {
  caseData: CaseDetailResponse
  isEditing: boolean
}

const props = defineProps<Props>()

const stageColorMap: Record<string, string> = {
  '待立案': 'bg-blue-100 text-blue-800',
  '审理准备': 'bg-cyan-100 text-cyan-800',
  '审理中': 'bg-yellow-100 text-yellow-800',
  '已结案': 'bg-green-100 text-green-800',
}

const stageBgColor = computed(() => stageColorMap[props.caseData.status] || 'bg-gray-100 text-gray-800')
</script>

<template>
  <Card>
    <CardHeader>
      <div class="flex items-start justify-between">
        <div class="space-y-2">
          <div class="flex items-center gap-3">
            <CardTitle class="text-2xl">{{ caseData.caseNumber }}</CardTitle>
            <Badge :class="stageBgColor">
              {{ caseData.status }}
            </Badge>
            <Badge v-if="caseData.archived" variant="secondary">
              已归档
            </Badge>
          </div>
          <p class="font-medium">{{ caseData.caseName }}</p>
          <p class="text-sm text-muted-foreground">{{ caseData.courtName || '法院待定' }}</p>
        </div>
      </div>
    </CardHeader>
    <CardContent>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div class="space-y-1">
          <p class="text-xs font-medium text-muted-foreground">案由</p>
          <p class="text-sm font-semibold">{{ caseData.caseCause || '待补充' }}</p>
        </div>
        <div class="space-y-1">
          <p class="text-xs font-medium text-muted-foreground">原告</p>
          <p class="text-sm font-semibold">{{ caseData.plaintiff }}</p>
        </div>
        <div class="space-y-1">
          <p class="text-xs font-medium text-muted-foreground">被告</p>
          <p class="text-sm font-semibold">{{ caseData.defendant }}</p>
        </div>
        <div class="space-y-1">
          <p class="text-xs font-medium text-muted-foreground">描述</p>
          <p class="text-sm text-muted-foreground line-clamp-2">
            {{ caseData.description || '暂无描述' }}
          </p>
        </div>
      </div>
    </CardContent>
  </Card>
</template>
