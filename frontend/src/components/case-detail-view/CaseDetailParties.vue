
<script setup lang="ts">
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import {
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import type { CaseDetailResponse } from '@/types/case'

interface Props {
  caseData: CaseDetailResponse
  isEditing: boolean
}

defineProps<Props>()
</script>

<template>
  <Card>
    <CardHeader>
      <CardTitle>当事人信息</CardTitle>
    </CardHeader>
    <CardContent class="space-y-6">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <!-- Plaintiff -->
        <FormField v-slot="{ componentField }" name="plaintiff">
          <FormItem>
            <FormLabel>原告/申请人 <span class="text-destructive">*</span></FormLabel>
            <FormControl>
              <Input
                v-bind="componentField"
                :disabled="!isEditing"
                placeholder="输入原告姓名或单位"
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        </FormField>

        <!-- Defendant -->
        <FormField v-slot="{ componentField }" name="defendant">
          <FormItem>
            <FormLabel>被告/被申请人 <span class="text-destructive">*</span></FormLabel>
            <FormControl>
              <Input
                v-bind="componentField"
                :disabled="!isEditing"
                placeholder="输入被告姓名或单位"
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        </FormField>
      </div>
    </CardContent>
  </Card>
</template>
