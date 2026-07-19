
<script setup lang="ts">
import { computed } from 'vue'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'
import type { CaseModel, CaseStage } from '@/data/case'

interface Props {
  caseData: CaseModel
  isEditing: boolean
}

const props = defineProps<Props>()

const stageColorMap: Record<CaseStage, string> = {
  '立案中': 'bg-blue-100 text-blue-800',
  '审理准备阶段': 'bg-cyan-100 text-cyan-800',
  '审理中': 'bg-yellow-100 text-yellow-800',
  '已判决(上诉期内)': 'bg-orange-100 text-orange-800',
  '上诉审理中': 'bg-purple-100 text-purple-800',
  '已判决(生效)': 'bg-green-100 text-green-800',
  '执行中': 'bg-indigo-100 text-indigo-800',
  '已归档': 'bg-gray-100 text-gray-800',
}

const stageBgColor = computed(() => stageColorMap[props.caseData.caseStage] || 'bg-gray-100 text-gray-800')
</script>

<template>
  <Card>
    <CardHeader>
      <div class="flex items-start justify-between">
        <div class="space-y-2">
          <div class="flex items-center gap-3">
            <CardTitle class="text-2xl">{{ caseData.caseNumber }}</CardTitle>
            <Badge :class="stageBgColor">
              {{ caseData.caseStage }}
            </Badge>
          </div>
          <p class="text-sm text-muted-foreground">{{ caseData.courtName }}</p>
        </div>
        <div class="flex gap-2">
          <div
            v-for="tag in caseData.tags"
            :key="tag.id"
            :class="[tag.color, 'px-3 py-1 rounded-full text-white text-xs font-medium']"
          >
            {{ tag.name }}
          </div>
        </div>
      </div>
    </CardHeader>
    <CardContent>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div class="space-y-1">
          <p class="text-xs font-medium text-muted-foreground">案由</p>
          <p class="text-sm font-semibold">{{ caseData.caseCause }}</p>
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
