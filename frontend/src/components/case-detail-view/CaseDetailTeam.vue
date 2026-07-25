
<script setup lang="ts">
import { computed } from 'vue'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import { MOCK_USERS, UserRole, UserStatus } from '@/data/user'
import type { CaseDetailResponse } from '@/types/case'

interface Props {
  caseData: CaseDetailResponse
  isEditing: boolean
}

const props = defineProps<Props>()

const leadLawyerOptions = computed(() => {
  const names = MOCK_USERS
    .filter(user => user.role === UserRole.LeadAttorney && user.status === UserStatus.Active)
    .map(user => user.name)

  if (!names.includes(props.caseData.leadLawyerName)) {
    names.unshift(props.caseData.leadLawyerName)
  }

  return names
})

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map(n => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
}

</script>

<template>
  <Card>
    <CardHeader>
      <CardTitle>团队成员</CardTitle>
    </CardHeader>
    <CardContent class="space-y-6">
      <!-- Lead Attorney -->
      <div class="space-y-3">
        <h4 class="font-medium text-sm">主办律师</h4>
        <FormField v-if="isEditing" v-slot="{ componentField }" name="leadLawyerName">
          <FormItem>
            <FormLabel>主办律师 <span class="text-destructive">*</span></FormLabel>
            <Select v-bind="componentField">
              <FormControl>
                <SelectTrigger>
                  <SelectValue placeholder="选择主办律师" />
                </SelectTrigger>
              </FormControl>
              <SelectContent>
                <SelectItem
                  v-for="lawyerName in leadLawyerOptions"
                  :key="lawyerName"
                  :value="lawyerName"
                >
                  {{ lawyerName }}
                </SelectItem>
              </SelectContent>
            </Select>
            <FormMessage />
          </FormItem>
        </FormField>
        <div v-else class="flex items-center gap-3 p-3 border rounded-lg">
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
