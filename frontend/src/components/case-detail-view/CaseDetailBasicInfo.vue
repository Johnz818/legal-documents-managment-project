
<script setup lang="ts">
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import {
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import { CASE_STATUS_OPTIONS } from '@/constants/caseStatus'
import type { CaseDetailResponse } from '@/types/case'

interface Props {
  caseData: CaseDetailResponse
  isEditing: boolean
}

defineProps<Props>()

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
        <FormField v-slot="{ componentField }" name="caseNumber">
          <FormItem>
            <FormLabel>案号 <span class="text-destructive">*</span></FormLabel>
            <FormControl>
              <Input
                v-bind="componentField"
                :disabled="!isEditing"
                placeholder="输入案号"
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        </FormField>

        <!-- Case Name -->
        <FormField v-slot="{ componentField }" name="caseName">
          <FormItem>
            <FormLabel>案件名称 <span class="text-destructive">*</span></FormLabel>
            <FormControl>
              <Input
                v-bind="componentField"
                :disabled="!isEditing"
                placeholder="输入案件名称"
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        </FormField>

        <!-- Court Name -->
        <FormField v-slot="{ componentField }" name="courtName">
          <FormItem>
            <FormLabel>法院名称</FormLabel>
            <FormControl>
              <Input
                v-bind="componentField"
                :disabled="!isEditing"
                placeholder="输入法院名称"
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        </FormField>

        <!-- Case Cause -->
        <FormField v-slot="{ componentField }" name="caseCause">
          <FormItem>
            <FormLabel>案由</FormLabel>
            <FormControl>
              <Input
                v-bind="componentField"
                :disabled="!isEditing"
                placeholder="输入案由"
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        </FormField>

        <!-- Case Stage -->
        <FormField v-slot="{ componentField }" name="status">
          <FormItem>
            <FormLabel>案件阶段 <span class="text-destructive">*</span></FormLabel>
            <Select v-bind="componentField" :disabled="!isEditing">
              <FormControl>
                <SelectTrigger>
                  <SelectValue placeholder="选择案件阶段" />
                </SelectTrigger>
              </FormControl>
              <SelectContent>
                <SelectItem
                  v-for="option in CASE_STATUS_OPTIONS"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </SelectItem>
              </SelectContent>
            </Select>
            <FormMessage />
          </FormItem>
        </FormField>
      </div>

      <!-- Description -->
      <FormField v-slot="{ componentField }" name="description">
        <FormItem>
          <FormLabel>案件描述</FormLabel>
          <FormControl>
            <Textarea
              v-bind="componentField"
              :disabled="!isEditing"
              placeholder="输入案件描述信息"
              class="min-h-24"
            />
          </FormControl>
          <FormMessage />
        </FormItem>
      </FormField>

      <div class="grid grid-cols-1 gap-4 border-t pt-4 text-sm text-muted-foreground md:grid-cols-2">
        <p>创建时间：{{ formatDateTime(caseData.createdAt) }}</p>
        <p>更新时间：{{ formatDateTime(caseData.updatedAt) }}</p>
      </div>
    </CardContent>
  </Card>
</template>
