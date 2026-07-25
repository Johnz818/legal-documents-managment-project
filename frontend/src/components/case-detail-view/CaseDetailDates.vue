
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
      <CardTitle>关键日期</CardTitle>
    </CardHeader>
    <CardContent class="space-y-6">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <!-- Filing Date -->
        <FormField v-slot="{ componentField }" name="filingDate">
          <FormItem>
            <FormLabel>立案时间</FormLabel>
            <FormControl>
              <Input
                v-bind="componentField"
                type="date"
                :disabled="!isEditing"
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        </FormField>

        <!-- Hearing Date -->
        <FormField v-slot="{ componentField }" name="hearingDate">
          <FormItem>
            <FormLabel>开庭时间</FormLabel>
            <FormControl>
              <Input
                v-bind="componentField"
                type="date"
                :disabled="!isEditing"
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        </FormField>

        <!-- Judgment Date -->
        <FormField v-slot="{ componentField }" name="judgmentDate">
          <FormItem>
            <FormLabel>判决时间</FormLabel>
            <FormControl>
              <Input
                v-bind="componentField"
                type="date"
                :disabled="!isEditing"
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        </FormField>
      </div>
    </CardContent>
  </Card>
</template>
