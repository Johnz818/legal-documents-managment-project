
<script setup lang="ts">
import { computed } from 'vue'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { MOCK_USERS } from '@/data/user'
import type { CaseModel } from '@/data/case'

interface Props {
  caseData: CaseModel
  isEditing: boolean
}

const props = defineProps<Props>()

const leadAttorney = computed(() => {
  return MOCK_USERS.find(u => u.id === props.caseData.leadAttorneyId)
})

const coAttorneys = computed(() => {
  return MOCK_USERS.filter(u => props.caseData.coAttorneysIds.includes(u.id))
})

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
        <div v-if="leadAttorney" class="flex items-center gap-3 p-3 border rounded-lg">
          <Avatar class="h-10 w-10">
            <AvatarFallback>{{ getInitials(leadAttorney.name) }}</AvatarFallback>
          </Avatar>
          <div class="flex-1 min-w-0">
            <p class="font-medium text-sm">{{ leadAttorney.name }}</p>
            <p class="text-xs text-muted-foreground">{{ leadAttorney.email }}</p>
          </div>
          <Badge variant="secondary">{{ leadAttorney.role }}</Badge>
        </div>
      </div>

      <!-- Co-Attorneys -->
      <div class="space-y-3">
        <h4 class="font-medium text-sm">协办律师</h4>
        <div v-if="coAttorneys.length > 0" class="space-y-2">
          <div
            v-for="attorney in coAttorneys"
            :key="attorney.id"
            class="flex items-center gap-3 p-3 border rounded-lg"
          >
            <Avatar class="h-10 w-10">
              <AvatarFallback>{{ getInitials(attorney.name) }}</AvatarFallback>
            </Avatar>
            <div class="flex-1 min-w-0">
              <p class="font-medium text-sm">{{ attorney.name }}</p>
              <p class="text-xs text-muted-foreground">{{ attorney.email }}</p>
            </div>
            <Badge variant="secondary">{{ attorney.role }}</Badge>
          </div>
        </div>
        <div v-else class="text-sm text-muted-foreground p-3 border rounded-lg border-dashed">
          暂无协办律师
        </div>
      </div>
    </CardContent>
  </Card>
</template>
