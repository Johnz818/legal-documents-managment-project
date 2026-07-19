
<script setup lang="ts">
import { ref } from 'vue'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { CaseModel } from '@/data/case'

interface Props {
  caseData: CaseModel
  isEditing: boolean
}

const props = defineProps<Props>()

const formData = ref({
  filingDate: props.caseData.filingDate || '',
  hearingDate: props.caseData.hearingDate || '',
  judgmentDate: props.caseData.judgmentDate || '',
})
</script>

<template>
  <Card>
    <CardHeader>
      <CardTitle>关键日期</CardTitle>
    </CardHeader>
    <CardContent class="space-y-6">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <!-- Filing Date -->
        <div class="space-y-2">
          <Label for="filingDate">立案时间</Label>
          <Input
            id="filingDate"
            v-model="formData.filingDate"
            type="date"
            :disabled="!isEditing"
          />
        </div>

        <!-- Hearing Date -->
        <div class="space-y-2">
          <Label for="hearingDate">开庭时间</Label>
          <Input
            id="hearingDate"
            v-model="formData.hearingDate"
            type="date"
            :disabled="!isEditing"
          />
        </div>

        <!-- Judgment Date -->
        <div class="space-y-2">
          <Label for="judgmentDate">判决时间</Label>
          <Input
            id="judgmentDate"
            v-model="formData.judgmentDate"
            type="date"
            :disabled="!isEditing"
          />
        </div>
      </div>
    </CardContent>
  </Card>
</template>
