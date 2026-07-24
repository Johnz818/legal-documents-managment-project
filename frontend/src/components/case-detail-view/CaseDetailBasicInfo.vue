
<script setup lang="ts">
import { ref } from 'vue'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import type { CaseDetailResponse } from '@/types/case'

interface Props {
  caseData: CaseDetailResponse
  isEditing: boolean
}

const props = defineProps<Props>()

const formData = ref({
  caseNumber: props.caseData.caseNumber,
  caseName: props.caseData.caseName,
  courtName: props.caseData.courtName ?? '',
  caseCause: props.caseData.caseCause ?? '',
  status: props.caseData.status,
  description: props.caseData.description ?? '',
})

const statusOptions = ['待立案', '审理准备', '审理中', '已结案']

const formatDateTime = (dateTime: string) => {
  return new Date(dateTime).toLocaleString('zh-CN')
}
</script>

<template>
  <Card>
    <CardHeader>
      <CardTitle>基本信息</CardTitle>
    </CardHeader>
    <CardContent class="space-y-6">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <!-- Case Number -->
        <div class="space-y-2">
          <Label for="caseNumber">案号</Label>
          <Input
            id="caseNumber"
            v-model="formData.caseNumber"
            :disabled="!isEditing"
            placeholder="输入案号"
          />
        </div>

        <!-- Case Name -->
        <div class="space-y-2">
          <Label for="caseName">案件名称</Label>
          <Input
            id="caseName"
            v-model="formData.caseName"
            :disabled="!isEditing"
            placeholder="输入案件名称"
          />
        </div>

        <!-- Court Name -->
        <div class="space-y-2">
          <Label for="courtName">法院名称</Label>
          <Input
            id="courtName"
            v-model="formData.courtName"
            :disabled="!isEditing"
            placeholder="输入法院名称"
          />
        </div>

        <!-- Case Cause -->
        <div class="space-y-2">
          <Label for="caseCause">案由</Label>
          <Input
            id="caseCause"
            v-model="formData.caseCause"
            :disabled="!isEditing"
            placeholder="输入案由"
          />
        </div>

        <!-- Case Stage -->
        <div class="space-y-2">
          <Label for="caseStage">案件阶段</Label>
          <Select v-model="formData.status" :disabled="!isEditing">
            <SelectTrigger id="caseStage">
              <SelectValue placeholder="选择案件阶段" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="status in statusOptions" :key="status" :value="status">
                {{ status }}
              </SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      <!-- Description -->
      <div class="space-y-2">
        <Label for="description">案件描述</Label>
        <Textarea
          id="description"
          v-model="formData.description"
          :disabled="!isEditing"
          placeholder="输入案件描述信息"
          class="min-h-24"
        />
      </div>

      <div class="grid grid-cols-1 gap-4 border-t pt-4 text-sm text-muted-foreground md:grid-cols-2">
        <p>创建时间：{{ formatDateTime(caseData.createdAt) }}</p>
        <p>更新时间：{{ formatDateTime(caseData.updatedAt) }}</p>
      </div>
    </CardContent>
  </Card>
</template>
