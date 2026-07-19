
<script setup lang="ts">
import { computed } from 'vue'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import SafeIcon from '@/components/common/SafeIcon.vue'
import type { ImportResultModel } from '@/data/case'

interface Props {
  result: ImportResultModel
}

defineProps<Props>()

const successRate = computed(() => {
  if (result.totalRecords === 0) return 0
  return Math.round((result.successCount / result.totalRecords) * 100)
})
</script>

<template>
  <div class="space-y-4">
    <!-- Success Summary -->
    <Card :class="result.failureCount === 0 ? 'border-green-200 bg-green-50' : 'border-yellow-200 bg-yellow-50'">
      <CardHeader>
        <CardTitle class="flex items-center gap-2">
          <SafeIcon 
            :name="result.failureCount === 0 ? 'CheckCircle' : 'AlertCircle'" 
            :size="24"
            :class="result.failureCount === 0 ? 'text-green-600' : 'text-yellow-600'"
          />
          <span :class="result.failureCount === 0 ? 'text-green-900' : 'text-yellow-900'">
            导入{{ result.failureCount === 0 ? '成功' : '部分成功' }}
          </span>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div class="grid grid-cols-3 gap-4">
          <div class="text-center">
            <p class="text-3xl font-bold text-primary">{{ result.totalRecords }}</p>
            <p class="text-sm text-muted-foreground mt-1">总记录数</p>
          </div>
          <div class="text-center">
            <p class="text-3xl font-bold text-green-600">{{ result.successCount }}</p>
            <p class="text-sm text-muted-foreground mt-1">成功导入</p>
          </div>
          <div class="text-center">
            <p class="text-3xl font-bold" :class="result.failureCount === 0 ? 'text-green-600' : 'text-red-600'">
              {{ result.failureCount }}
            </p>
            <p class="text-sm text-muted-foreground mt-1">导入失败</p>
          </div>
        </div>

        <!-- Progress Bar -->
        <div class="mt-6 space-y-2">
          <div class="flex justify-between text-sm">
            <span class="font-medium">成功率</span>
            <span class="font-semibold">{{ successRate }}%</span>
          </div>
          <div class="h-2 rounded-full bg-muted overflow-hidden">
            <div 
              class="h-full bg-green-500 transition-all duration-500"
              :style="{ width: `${successRate}%` }"
            />
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- Failure Details -->
    <div v-if="result.failureCount > 0" class="space-y-3">
      <Alert variant="destructive">
        <SafeIcon name="AlertTriangle" :size="16" />
        <AlertTitle>导入失败详情</AlertTitle>
        <AlertDescription>
          以下记录导入失败，请检查数据后重新上传
        </AlertDescription>
      </Alert>

      <Card class="border-red-200">
        <CardHeader>
          <CardTitle class="text-base">失败原因</CardTitle>
        </CardHeader>
        <CardContent>
          <ul class="space-y-2">
            <li v-for="(reason, index) in result.failedReasons" :key="index" class="flex items-start gap-3">
              <Badge variant="destructive" class="mt-1 shrink-0">{{ index + 1 }}</Badge>
              <span class="text-sm text-muted-foreground">{{ reason }}</span>
            </li>
          </ul>
        </CardContent>
      </Card>

      <Alert class="bg-blue-50 border-blue-200">
        <SafeIcon name="Info" :size="16" class="text-blue-600" />
        <AlertTitle class="text-blue-900">建议</AlertTitle>
        <AlertDescription class="text-blue-800">
          请根据上述失败原因修正Excel文件中的数据，然后重新上传。您可以下载失败记录详情进行修正。
        </AlertDescription>
      </Alert>
    </div>

    <!-- Success Message -->
    <div v-else class="rounded-lg border border-green-200 bg-green-50 p-4">
      <div class="flex items-center gap-3">
        <SafeIcon name="CheckCircle" :size="20" class="text-green-600" />
        <div>
          <p class="font-medium text-green-900">所有案件导入成功！</p>
          <p class="text-sm text-green-700">{{ result.successCount }} 个案件已添加到系统中</p>
        </div>
      </div>
    </div>
  </div>
</template>
