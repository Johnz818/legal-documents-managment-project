
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
import type { CaseModel, CaseStage } from '@/data/case'
import { CaseStage as CaseStageEnum } from '@/data/case'

interface Props {
  caseData: CaseModel
  isEditing: boolean
}

const props = defineProps<Props>()

const formData = ref({
  caseNumber: props.caseData.caseNumber,
  courtName: props.caseData.courtName,
  caseCause: props.caseData.caseCause,
  caseStage: props.caseData.caseStage,
  description: props.caseData.description || '',
})

const stageOptions = Object.values(CaseStageEnum)
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
          <Select v-model="formData.caseStage" :disabled="!isEditing">
            <SelectTrigger id="caseStage">
              <SelectValue placeholder="选择案件阶段" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="stage in stageOptions" :key="stage" :value="stage">
                {{ stage }}
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
    </CardContent>
  </Card>
</template>
