
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
  plaintiff: props.caseData.plaintiff,
  defendant: props.caseData.defendant,
})
</script>

<template>
  <Card>
    <CardHeader>
      <CardTitle>当事人信息</CardTitle>
    </CardHeader>
    <CardContent class="space-y-6">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <!-- Plaintiff -->
        <div class="space-y-2">
          <Label for="plaintiff">原告/申请人</Label>
          <Input
            id="plaintiff"
            v-model="formData.plaintiff"
            :disabled="!isEditing"
            placeholder="输入原告姓名或单位"
          />
        </div>

        <!-- Defendant -->
        <div class="space-y-2">
          <Label for="defendant">被告/被申请人</Label>
          <Input
            id="defendant"
            v-model="formData.defendant"
            :disabled="!isEditing"
            placeholder="输入被告姓名或单位"
          />
        </div>
      </div>
    </CardContent>
  </Card>
</template>
