<script setup lang="ts">
import { computed, ref } from 'vue'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ScrollArea } from '@/components/ui/scroll-area'
import SafeIcon from '@/components/common/SafeIcon.vue'
import type { CaseSummaryResponse } from '@/types/case'

const props = defineProps<{
  cases: CaseSummaryResponse[]
  selectedCaseId: number | null
  isLoading: boolean
  errorMessage: string
  disabled: boolean
}>()

const emit = defineEmits<{
  select: [caseId: number]
  retry: []
}>()

const searchQuery = ref('')

const filteredCases = computed(() => {
  const query = searchQuery.value.trim().toLocaleLowerCase()
  if (!query) return props.cases
  return props.cases.filter(item => [
    item.caseNumber,
    item.caseName,
    item.courtName,
    item.caseCause,
    item.plaintiff,
    item.leadLawyerName,
  ].some(value => value?.toLocaleLowerCase().includes(query)))
})
</script>

<template>
  <div class="space-y-4">
    <div class="relative">
      <SafeIcon name="Search" :size="16" class="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
      <Input v-model="searchQuery" placeholder="搜索案号、案件名称、法院或当事人..." class="pl-9" :disabled="isLoading || disabled" />
    </div>

    <div v-if="isLoading" class="flex h-[320px] items-center justify-center gap-2 rounded-lg border text-sm text-muted-foreground">
      <SafeIcon name="LoaderCircle" :size="18" class="animate-spin" />
      正在加载案件...
    </div>

    <div v-else-if="errorMessage" role="alert" class="flex h-[320px] flex-col items-center justify-center gap-3 rounded-lg border p-6 text-center">
      <p class="text-sm text-destructive">{{ errorMessage }}</p>
      <Button variant="outline" size="sm" @click="emit('retry')">重试</Button>
    </div>

    <ScrollArea v-else class="h-[320px] rounded-lg border p-4">
      <div class="space-y-2">
        <button
          v-for="caseItem in filteredCases"
          :key="caseItem.id"
          type="button"
          :disabled="disabled"
          class="w-full rounded-lg border p-3 text-left transition-all hover:bg-accent disabled:cursor-not-allowed disabled:opacity-60"
          :class="selectedCaseId === caseItem.id ? 'border-primary bg-primary/10' : ''"
          @click="emit('select', caseItem.id)"
        >
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0 flex-1">
              <div class="mb-1 flex items-center gap-2">
                <p class="truncate text-sm font-semibold">{{ caseItem.caseNumber }}</p>
                <Badge variant="outline" class="shrink-0 text-xs">{{ caseItem.status }}</Badge>
              </div>
              <p class="truncate text-xs text-muted-foreground">{{ caseItem.caseName }}</p>
              <p class="mt-2 text-xs"><span class="text-muted-foreground">当事人：</span>{{ caseItem.plaintiff }}</p>
              <p class="mt-1 text-xs"><span class="text-muted-foreground">主办律师：</span>{{ caseItem.leadLawyerName }}</p>
            </div>
            <SafeIcon v-if="selectedCaseId === caseItem.id" name="CircleCheck" :size="20" class="shrink-0 text-primary" />
          </div>
        </button>

        <div v-if="filteredCases.length === 0" class="py-10 text-center text-sm text-muted-foreground">
          未找到匹配的案件
        </div>
      </div>
    </ScrollArea>

    <p v-if="!isLoading && !errorMessage" class="text-xs text-muted-foreground">共 {{ filteredCases.length }} 个案件</p>
  </div>
</template>
