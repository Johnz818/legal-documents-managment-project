
<script setup lang="ts">
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import type { CaseDetailResponse } from '@/types/case'

interface Props {
  caseData: CaseDetailResponse
  isEditing: boolean
}

const props = defineProps<Props>()

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map(n => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
}

const handleEditTeam = () => {
  if (typeof window !== 'undefined') {
    window.location.href = `./user-list.html?caseId=${props.caseData.id}`
  }
}
</script>

<template>
  <Card>
    <CardHeader class="flex flex-row items-center justify-between">
      <CardTitle>团队成员</CardTitle>
      <Button
        v-if="isEditing"
        variant="outline"
        size="sm"
        @click="handleEditTeam"
      >
        <SafeIcon name="Edit" :size="16" class="mr-2" />
        编辑团队
      </Button>
    </CardHeader>
    <CardContent class="space-y-6">
      <!-- Lead Attorney -->
      <div class="space-y-3">
        <h4 class="font-medium text-sm">主办律师</h4>
        <div class="flex items-center gap-3 p-3 border rounded-lg">
          <Avatar class="h-10 w-10">
            <AvatarFallback>{{ getInitials(caseData.leadLawyerName) }}</AvatarFallback>
          </Avatar>
          <div class="flex-1 min-w-0">
            <p class="font-medium text-sm">{{ caseData.leadLawyerName }}</p>
            <p class="text-xs text-muted-foreground">案件主办律师</p>
          </div>
          <Badge variant="secondary">主办律师</Badge>
        </div>
      </div>

      <!-- Co-Attorneys -->
      <div class="space-y-3">
        <h4 class="font-medium text-sm">协办律师</h4>
        <div class="text-sm text-muted-foreground p-3 border rounded-lg border-dashed">
          协办成员功能尚未接入
        </div>
      </div>
    </CardContent>
  </Card>
</template>
